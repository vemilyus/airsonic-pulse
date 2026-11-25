package org.airsonic.player.security;

import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds the OIDC client registration from {@link SettingsService} (environment, properties file or
 * Advanced Settings). The registration is created once at startup; changes need a restart.
 */
@Configuration
public class OidcSecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OidcSecurityConfig.class);

    /** Registration id, part of the authorization and redirect URIs. */
    public static final String REGISTRATION_ID = "airsonic";

    public static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    @Bean
    public OidcClientRegistrationHolder oidcClientRegistrationHolder(SettingsService settingsService) {
        if (!settingsService.isOidcConfigured()) {
            if (settingsService.isOidcEnabled()) {
                LOG.warn("OIDC is enabled but the issuer URI or client id is missing; OIDC login is disabled.");
            }
            return OidcClientRegistrationHolder.disabled();
        }
        try {
            ClientRegistration registration = buildRegistration(settingsService);
            LOG.info("OIDC login enabled for issuer {}", settingsService.getOidcIssuerUri());
            return OidcClientRegistrationHolder.of(new InMemoryClientRegistrationRepository(registration));
        } catch (Exception e) {
            LOG.error("OIDC provider at {} could not be configured; OIDC login is disabled for this run.",
                    settingsService.getOidcIssuerUri(), e);
            return OidcClientRegistrationHolder.disabled();
        }
    }

    static ClientRegistration buildRegistration(SettingsService settingsService) {
        ClientRegistration.Builder builder = ClientRegistrations
                .fromIssuerLocation(settingsService.getOidcIssuerUri().trim())
                .registrationId(REGISTRATION_ID)
                .clientId(settingsService.getOidcClientId().trim())
                .clientName(settingsService.getOidcButtonLabel())
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(scopes(settingsService.getOidcScopes()));

        String secret = settingsService.getOidcClientSecret();
        if (StringUtils.isNotEmpty(secret)) {
            builder.clientSecret(secret);
        } else {
            // public client, relies on PKCE only
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        }
        return builder.build();
    }

    static Set<String> scopes(String configured) {
        Set<String> scopes = new LinkedHashSet<>();
        scopes.add("openid");
        if (StringUtils.isNotBlank(configured)) {
            for (String scope : configured.trim().split("[\\s,]+")) {
                if (!scope.isEmpty()) {
                    scopes.add(scope);
                }
            }
        }
        return scopes;
    }
}
