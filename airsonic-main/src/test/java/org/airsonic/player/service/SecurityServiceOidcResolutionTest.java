package org.airsonic.player.service;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.User.Role;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.repository.UserCredentialRepository;
import org.airsonic.player.repository.UserRepository;
import org.airsonic.player.util.FixtureTableGuardExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * Resolution matrix of {@link SecurityService#resolveOidcUser}: existing link, link by verified
 * email, auto-create, reject.
 */
@SpringBootTest
@ExtendWith(FixtureTableGuardExtension.class)
@EnableConfigurationProperties({ AirsonicHomeConfig.class })
public class SecurityServiceOidcResolutionTest {

    private static final String ISSUER = "https://idp.example.com/realms/test";
    private static final String LINKED_USER = "oidcLinkedUser";
    private static final String EMAIL_USER = "oidcEmailUser";
    private static final String EMAIL = "oidc-email-user@example.com";

    @TempDir
    private static Path tempDir;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @MockitoSpyBean
    private SettingsService settingsService;

    private final List<String> createdUsers = new ArrayList<>();

    @BeforeAll
    public static void setup() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @BeforeEach
    public void beforeEach() {
        createUser(LINKED_USER, null);
        createUser(EMAIL_USER, EMAIL);
    }

    @AfterEach
    public void afterEach() {
        createdUsers.forEach(u -> {
            userCredentialRepository.deleteAll(userCredentialRepository.findByUserUsernameAndApp(u, App.OIDC));
            userRepository.deleteById(u);
        });
        createdUsers.clear();
    }

    private void createUser(String name, String email) {
        userRepository.saveAndFlush(new User(name, email, false, 0L, 0L, 0L, Set.of(Role.STREAM)));
        createdUsers.add(name);
    }

    private static Map<String, Object> claims(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    public void existingLinkResolvesUser() {
        assertTrue(securityService.createOidcCredential(LINKED_USER, ISSUER, "sub-linked", null));

        Optional<User> user = securityService.resolveOidcUser(ISSUER, "sub-linked", claims());

        assertEquals(LINKED_USER, user.orElseThrow().getUsername());
    }

    @Test
    public void linkOfAnotherIssuerIsNotUsed() {
        assertTrue(securityService.createOidcCredential(LINKED_USER, ISSUER, "sub-linked", null));

        assertTrue(securityService.resolveOidcUser("https://other.example.com", "sub-linked", claims()).isEmpty());
    }

    @Test
    public void expiredLinkIsRejected() {
        assertTrue(securityService.createOidcCredential(LINKED_USER, ISSUER, "sub-expired", null));
        UserCredential uc = securityService.getOidcCredential(ISSUER, "sub-expired").orElseThrow();
        uc.setExpiration(Instant.now().minusSeconds(60));
        userCredentialRepository.saveAndFlush(uc);

        assertTrue(securityService.resolveOidcUser(ISSUER, "sub-expired", claims()).isEmpty());
    }

    @Test
    public void unlinkedUserIsRejectedByDefault() {
        Optional<User> user = securityService.resolveOidcUser(ISSUER, "sub-unknown",
                claims("email", EMAIL, "email_verified", true, "preferred_username", "newbie"));

        assertTrue(user.isEmpty());
        assertTrue(userRepository.findByUsername("newbie").isEmpty());
        assertTrue(securityService.getOidcCredential(ISSUER, "sub-unknown").isEmpty());
    }

    @Test
    public void linkByVerifiedEmailCreatesCredential() {
        doReturn(true).when(settingsService).isOidcLinkByEmail();

        Optional<User> user = securityService.resolveOidcUser(ISSUER, "sub-email",
                claims("email", EMAIL, "email_verified", true));

        assertEquals(EMAIL_USER, user.orElseThrow().getUsername());
        assertEquals(EMAIL_USER, securityService.getOidcCredential(ISSUER, "sub-email").orElseThrow().getUser().getUsername());
    }

    @Test
    public void linkByEmailAcceptsStringVerifiedFlagAndIgnoresCase() {
        doReturn(true).when(settingsService).isOidcLinkByEmail();

        Optional<User> user = securityService.resolveOidcUser(ISSUER, "sub-email2",
                claims("email", EMAIL.toUpperCase(), "email_verified", "true"));

        assertEquals(EMAIL_USER, user.orElseThrow().getUsername());
    }

    @Test
    public void unverifiedEmailIsNeverLinked() {
        doReturn(true).when(settingsService).isOidcLinkByEmail();

        assertTrue(securityService.resolveOidcUser(ISSUER, "sub-unverified",
                claims("email", EMAIL, "email_verified", false)).isEmpty());
        assertTrue(securityService.resolveOidcUser(ISSUER, "sub-unverified",
                claims("email", EMAIL)).isEmpty());
        assertTrue(securityService.getOidcCredential(ISSUER, "sub-unverified").isEmpty());
    }

    @Test
    public void verifiedEmailIsNotLinkedWhenFeatureOff() {
        assertTrue(securityService.resolveOidcUser(ISSUER, "sub-off",
                claims("email", EMAIL, "email_verified", true)).isEmpty());
    }

    @Test
    public void duplicateEmailIsRejected() {
        doReturn(true).when(settingsService).isOidcLinkByEmail();
        createUser("oidcEmailUser2", EMAIL);

        assertTrue(securityService.resolveOidcUser(ISSUER, "sub-dup",
                claims("email", EMAIL, "email_verified", true)).isEmpty());
    }

    @Test
    public void autoCreateMakesUserWithDefaultRolesAndNoPasswordAuth() {
        doReturn(true).when(settingsService).isOidcAutoCreateUsers();
        createdUsers.add("oidc-new-user");

        Optional<User> user = securityService.resolveOidcUser(ISSUER, "sub-new",
                claims("preferred_username", "oidc-new-user", "email", "new@example.com"));

        User created = user.orElseThrow();
        assertEquals("oidc-new-user", created.getUsername());
        assertEquals(Set.of(Role.STREAM, Role.SETTINGS), created.getRoles());
        assertFalse(created.isPasswordAuthEnabled());
        assertFalse(created.isLdapAuthenticated());
        assertEquals("new@example.com", created.getEmail());
        assertEquals("oidc-new-user", securityService.getOidcCredential(ISSUER, "sub-new").orElseThrow().getUser().getUsername());
        // a second login resolves the same user through the link
        assertEquals("oidc-new-user", securityService.resolveOidcUser(ISSUER, "sub-new", claims()).orElseThrow().getUsername());
    }

    @Test
    public void autoCreateWithoutUsernameClaimIsRejected() {
        doReturn(true).when(settingsService).isOidcAutoCreateUsers();

        assertTrue(securityService.resolveOidcUser(ISSUER, "sub-nouser", claims("email", "x@example.com")).isEmpty());
        assertTrue(securityService.getOidcCredential(ISSUER, "sub-nouser").isEmpty());
    }

    @Test
    public void autoCreateDoesNotHijackExistingUnlinkedUsername() {
        doReturn(true).when(settingsService).isOidcAutoCreateUsers();

        assertTrue(securityService.resolveOidcUser(ISSUER, "sub-hijack",
                claims("preferred_username", LINKED_USER)).isEmpty());
        assertTrue(securityService.getOidcCredential(ISSUER, "sub-hijack").isEmpty());
    }

    @Test
    public void adminGroupMemberGetsAdminRole() {
        doReturn(true).when(settingsService).isOidcAutoCreateUsers();
        doReturn("groups").when(settingsService).getOidcGroupsClaim();
        doReturn("airsonic-admins").when(settingsService).getOidcAdminGroup();
        createdUsers.add("oidc-admin");

        User admin = securityService.resolveOidcUser(ISSUER, "sub-admin",
                claims("preferred_username", "oidc-admin", "groups", List.of("airsonic-admins"))).orElseThrow();
        assertTrue(admin.getRoles().contains(Role.ADMIN));
        assertTrue(admin.getRoles().containsAll(Set.of(Role.STREAM, Role.SETTINGS)));

        // an existing linked user is promoted on login when in the group
        assertTrue(securityService.createOidcCredential(LINKED_USER, ISSUER, "sub-linked-admin", null));
        User promoted = securityService.resolveOidcUser(ISSUER, "sub-linked-admin",
                claims("groups", List.of("airsonic-admins"))).orElseThrow();
        assertTrue(promoted.getRoles().contains(Role.ADMIN));
        assertTrue(securityService.getUserByName(LINKED_USER).isAdminRole());
    }

    @Test
    public void nonMemberDoesNotGetAdminRole() {
        doReturn("groups").when(settingsService).getOidcGroupsClaim();
        doReturn("airsonic-admins").when(settingsService).getOidcAdminGroup();
        assertTrue(securityService.createOidcCredential(LINKED_USER, ISSUER, "sub-plain", null));

        User user = securityService.resolveOidcUser(ISSUER, "sub-plain",
                claims("groups", List.of("airsonic-users"))).orElseThrow();

        assertFalse(user.getRoles().contains(Role.ADMIN));
    }

    @Test
    public void blankIssuerOrSubjectIsRejected() {
        assertTrue(securityService.resolveOidcUser(null, "sub", claims()).isEmpty());
        assertTrue(securityService.resolveOidcUser(ISSUER, " ", claims()).isEmpty());
    }
}
