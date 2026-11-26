package org.airsonic.player.security;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.util.FixtureTableGuardExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the web security chain with OIDC active, against a stubbed client registration (no real
 * identity provider is contacted).
 */
@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(FixtureTableGuardExtension.class)
@EnableConfigurationProperties({ AirsonicHomeConfig.class })
@Import(OidcLoginIntegrationTest.OidcTestConfig.class)
public class OidcLoginIntegrationTest {

    @TestConfiguration
    static class OidcTestConfig {
        @Bean
        @Primary
        OidcClientRegistrationHolder testOidcClientRegistrationHolder() {
            ClientRegistration registration = ClientRegistration.withRegistrationId(OidcSecurityConfig.REGISTRATION_ID)
                    .clientId("test-client")
                    .clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://idp.example.com/authorize")
                    .tokenUri("https://idp.example.com/token")
                    .jwkSetUri("https://idp.example.com/jwks")
                    .userInfoUri("https://idp.example.com/userinfo")
                    .userNameAttributeName("sub")
                    .clientName("Test IdP")
                    .build();
            return OidcClientRegistrationHolder.of(new InMemoryClientRegistrationRepository(registration));
        }
    }

    @TempDir
    private static Path tempDir;

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @Test
    public void authorizationEndpointRedirectsToProviderWithPkce() throws Exception {
        MvcResult result = mvc.perform(get("/oauth2/authorization/" + OidcSecurityConfig.REGISTRATION_ID))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        assertTrue(location.startsWith("https://idp.example.com/authorize?"), location);
        assertTrue(location.contains("response_type=code"), location);
        assertTrue(location.contains("client_id=test-client"), location);
        assertTrue(location.contains("code_challenge="), location);
        assertTrue(location.contains("code_challenge_method=S256"), location);
        assertTrue(location.contains("state="), location);
        assertTrue(location.contains("nonce="), location);
        assertTrue(location.contains("scope=openid"), location);
    }

    @Test
    public void loginPageShowsSsoButton() throws Exception {
        String html = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("id=\"oidc-login\""), html);
        assertTrue(html.contains("href=\"/oauth2/authorization/airsonic\""), html);
    }

    @Test
    public void callbackWithoutAuthorizationRequestFailsToLoginError() throws Exception {
        mvc.perform(get("/login/oauth2/code/" + OidcSecurityConfig.REGISTRATION_ID)
                .param("error", "access_denied"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    public void protectedPagesStillRequireAuthentication() throws Exception {
        mvc.perform(get("/index")).andExpect(status().isUnauthorized());
    }

    @Test
    public void formLoginStillWorks() throws Exception {
        mvc.perform(formLogin("/login").user("j_username", "admin").password("j_password", "admin"))
                .andExpect(authenticated().withUsername("admin"));
    }

    @Test
    public void unknownRegistrationIsNotServed() throws Exception {
        MvcResult result = mvc.perform(get("/oauth2/authorization/unknown")).andReturn();
        String location = result.getResponse().getRedirectedUrl();
        assertFalse(location != null && location.startsWith("https://idp.example.com"), String.valueOf(location));
    }
}
