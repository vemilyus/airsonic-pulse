package org.airsonic.player.security;

import org.airsonic.player.domain.User;
import org.airsonic.player.domain.User.Role;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AirsonicOidcUserServiceTest {

    private static final String ISSUER = "https://idp.example.com";

    @Mock
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    @Mock
    private SecurityService securityService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private OidcUserRequest request;

    private AirsonicOidcUserService service;

    @BeforeEach
    void setUp() {
        service = new AirsonicOidcUserService(delegate, securityService, settingsService);
        lenient().when(settingsService.getOidcRequiredClaim()).thenReturn("");
        lenient().when(settingsService.getOidcRequiredClaimValue()).thenReturn("");
    }

    private void idpReturns(Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "subject-1");
        claims.put("iss", ISSUER);
        claims.putAll(extraClaims);
        OidcIdToken token = new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(60), claims);
        when(delegate.loadUser(request)).thenReturn(new DefaultOidcUser(List.of(), token));
    }

    private static User user(String name, Role... roles) {
        return new User(name, null, false, 0L, 0L, 0L, Set.of(roles));
    }

    @Test
    void gateDisabledResolvesUserAndNamesPrincipalAfterAirsonicUser() {
        idpReturns(Map.of("preferred_username", "idp-name"));
        when(securityService.resolveOidcUser(eq(ISSUER), eq("subject-1"), anyMap()))
                .thenReturn(Optional.of(user("local-alice", Role.STREAM, Role.SETTINGS)));

        OidcUser result = service.loadUser(request);

        assertEquals("local-alice", result.getName());
        List<String> authorities = result.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(authorities.containsAll(List.of("ROLE_USER", "ROLE_STREAM", "ROLE_SETTINGS")));
        assertEquals(SecurityService.getGrantedAuthorities(user("local-alice", Role.STREAM, Role.SETTINGS)).size(),
                authorities.size());
        assertEquals("subject-1", result.getSubject());
    }

    @Test
    void gatePassedWithArrayClaim() {
        when(settingsService.getOidcRequiredClaim()).thenReturn("groups");
        when(settingsService.getOidcRequiredClaimValue()).thenReturn("airsonic-users");
        idpReturns(Map.of("groups", List.of("x", "airsonic-users")));
        when(securityService.resolveOidcUser(eq(ISSUER), eq("subject-1"), anyMap()))
                .thenReturn(Optional.of(user("bob", Role.STREAM)));

        assertEquals("bob", service.loadUser(request).getName());
    }

    @Test
    void gateRejectionHappensBeforeAnyUserResolution() {
        when(settingsService.getOidcRequiredClaim()).thenReturn("groups");
        when(settingsService.getOidcRequiredClaimValue()).thenReturn("airsonic-users");
        idpReturns(Map.of("groups", List.of("other")));

        OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
                () -> service.loadUser(request));

        assertEquals(AirsonicOidcUserService.ACCESS_DENIED, ex.getError().getErrorCode());
        verifyNoInteractions(securityService);
    }

    @Test
    void gateRejectsWhenClaimMissingEvenForAlreadyLinkedUser() {
        when(settingsService.getOidcRequiredClaim()).thenReturn("groups");
        when(settingsService.getOidcRequiredClaimValue()).thenReturn("airsonic-users");
        idpReturns(Map.of());

        assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(request));

        verify(securityService, never()).resolveOidcUser(any(), any(), any());
        verify(securityService, never()).createOidcCredential(any(), any(), any(), any());
    }

    @Test
    void unresolvedUserIsRejected() {
        idpReturns(Map.of());
        when(securityService.resolveOidcUser(eq(ISSUER), eq("subject-1"), anyMap())).thenReturn(Optional.empty());

        OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
                () -> service.loadUser(request));

        assertEquals(AirsonicOidcUserService.INVALID_USER, ex.getError().getErrorCode());
    }
}
