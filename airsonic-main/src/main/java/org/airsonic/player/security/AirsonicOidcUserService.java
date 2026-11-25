package org.airsonic.player.security;

import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the Airsonic user for an OIDC login. The required-claim gate runs first; only then the
 * local user is resolved or provisioned. The returned principal is named after the Airsonic user so
 * that {@code Principal.getName()} keeps working everywhere.
 */
@Component
public class AirsonicOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final Logger LOG = LoggerFactory.getLogger(AirsonicOidcUserService.class);

    /** Attribute holding the Airsonic username, used as the principal name attribute. */
    public static final String USERNAME_ATTRIBUTE = "airsonic_username";

    static final String ACCESS_DENIED = "access_denied";
    static final String INVALID_USER = "invalid_user";

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final SecurityService securityService;
    private final SettingsService settingsService;

    @Autowired
    public AirsonicOidcUserService(SecurityService securityService, SettingsService settingsService) {
        this(new OidcUserService(), securityService, settingsService);
    }

    AirsonicOidcUserService(OAuth2UserService<OidcUserRequest, OidcUser> delegate, SecurityService securityService,
            SettingsService settingsService) {
        this.delegate = delegate;
        this.securityService = securityService;
        this.settingsService = settingsService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        Map<String, Object> claims = oidcUser.getClaims();
        String subject = oidcUser.getSubject();

        // 1. access gate, before anything is looked up or created
        String requiredClaim = settingsService.getOidcRequiredClaim();
        if (StringUtils.isNotBlank(requiredClaim)
                && !OidcClaimMatcher.matches(claims, requiredClaim, settingsService.getOidcRequiredClaimValue())) {
            LOG.warn("OIDC sign-in rejected for subject '{}': claim '{}' does not satisfy the required value",
                    subject, requiredClaim);
            throw new OAuth2AuthenticationException(new OAuth2Error(ACCESS_DENIED),
                    "Required OIDC claim not satisfied");
        }

        // 2. resolve (or provision) the local user
        String issuer = oidcUser.getIdToken().getClaimAsString("iss");
        if (StringUtils.isBlank(issuer) && oidcUser.getIdToken().getIssuer() != null) {
            issuer = oidcUser.getIdToken().getIssuer().toString();
        }
        Optional<User> user = securityService.resolveOidcUser(issuer, subject, claims);
        if (user.isEmpty()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(INVALID_USER), "No matching Airsonic user");
        }

        // 3. principal named after the Airsonic user, authorities from Airsonic roles
        User airsonicUser = user.get();
        List<GrantedAuthority> authorities = SecurityService.getGrantedAuthorities(airsonicUser);
        OidcIdToken original = oidcUser.getIdToken();
        Map<String, Object> idTokenClaims = new HashMap<>(original.getClaims());
        idTokenClaims.put(USERNAME_ATTRIBUTE, airsonicUser.getUsername());
        OidcIdToken idToken = new OidcIdToken(original.getTokenValue(), original.getIssuedAt(),
                original.getExpiresAt(), idTokenClaims);
        return new DefaultOidcUser(authorities, idToken, oidcUser.getUserInfo(), USERNAME_ATTRIBUTE);
    }
}
