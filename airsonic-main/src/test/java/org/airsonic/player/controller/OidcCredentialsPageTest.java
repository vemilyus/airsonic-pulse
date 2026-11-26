package org.airsonic.player.controller;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.User.Role;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.repository.UserCredentialRepository;
import org.airsonic.player.repository.UserRepository;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.util.FixtureTableGuardExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The credentials page lists OIDC links read-only, never offers OIDC in the create dialog and lets the
 * user revoke a link.
 */
@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(FixtureTableGuardExtension.class)
@EnableConfigurationProperties({ AirsonicHomeConfig.class })
public class OidcCredentialsPageTest {

    private static final String USERNAME = "oidcCredentialsPageUser";
    private static final String ISSUER = "https://idp.example.com/realms/page";

    @TempDir
    private static Path tempDir;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @BeforeEach
    public void setUp() {
        userRepository.saveAndFlush(new User(USERNAME, null, false, 0L, 0L, 0L, Set.of(Role.SETTINGS)));
    }

    @AfterEach
    public void tearDown() {
        userCredentialRepository.deleteAll(userCredentialRepository.findByUserUsernameAndApp(USERNAME, App.OIDC));
        userRepository.deleteById(USERNAME);
    }

    @Test
    public void listsOidcLinkAndDoesNotOfferOidcInCreateDialog() throws Exception {
        assertTrue(securityService.createOidcCredential(USERNAME, ISSUER, "page-subject", "OIDC link"));

        String html = mvc.perform(get("/credentialsSettings.view")
                .with(user(USERNAME).roles("USER", "SETTINGS")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("id=\"oidcCredsNote\""), "OIDC section with note");
        assertTrue(html.contains("class=\"oidc-cred\""), "OIDC row");
        assertTrue(html.contains(ISSUER), "issuer shown");
        assertFalse(html.contains("<option value=\"OIDC\""), "OIDC must not be offered in the create dialog");
        assertTrue(html.contains("<option value=\"LASTFM\""), "other apps still offered");
    }

    @Test
    public void withoutLinksNoOidcSection() throws Exception {
        String html = mvc.perform(get("/credentialsSettings.view")
                .with(user(USERNAME).roles("USER", "SETTINGS")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(html.contains("class=\"oidc-cred\""));
        assertFalse(html.contains("id=\"oidcCredsNote\""));
    }

    @Test
    public void userCanRevokeOidcLink() throws Exception {
        assertTrue(securityService.createOidcCredential(USERNAME, ISSUER, "revoke-subject", "OIDC link"));
        UserCredential link = securityService.getOidcCredential(ISSUER, "revoke-subject").orElseThrow();

        mvc.perform(post("/credentialsSettings/update")
                .with(user(USERNAME).roles("USER", "SETTINGS"))
                .with(csrf())
                .param("credentials[0].hash", String.valueOf(link.hashCode()))
                .param("credentials[0].encoder", "noop")
                .param("credentials[0].markedForDeletion", "true"))
                .andExpect(status().is3xxRedirection());

        assertTrue(securityService.getOidcCredential(ISSUER, "revoke-subject").isEmpty(),
                "deleting the credential revokes single sign-on");
    }
}
