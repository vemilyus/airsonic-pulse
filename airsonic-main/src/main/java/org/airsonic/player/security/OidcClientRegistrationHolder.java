package org.airsonic.player.security;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Holds the OIDC {@link ClientRegistrationRepository} when OIDC is enabled, configured and the
 * provider could be reached at startup. Otherwise OIDC is not active for this run.
 */
public final class OidcClientRegistrationHolder {

    private static final OidcClientRegistrationHolder DISABLED = new OidcClientRegistrationHolder(null);

    private final ClientRegistrationRepository repository;

    private OidcClientRegistrationHolder(ClientRegistrationRepository repository) {
        this.repository = repository;
    }

    public static OidcClientRegistrationHolder disabled() {
        return DISABLED;
    }

    public static OidcClientRegistrationHolder of(ClientRegistrationRepository repository) {
        return new OidcClientRegistrationHolder(repository);
    }

    public boolean isAvailable() {
        return repository != null;
    }

    public ClientRegistrationRepository getRepository() {
        return repository;
    }
}
