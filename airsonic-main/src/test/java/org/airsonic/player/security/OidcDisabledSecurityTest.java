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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With OIDC disabled (the default) no OIDC endpoints are active and form login is unchanged.
 */
@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(FixtureTableGuardExtension.class)
@EnableConfigurationProperties({ AirsonicHomeConfig.class })
public class OidcDisabledSecurityTest {

    @TempDir
    private static Path tempDir;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OidcClientRegistrationHolder oidcClientRegistrations;

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @Test
    public void oidcIsNotAvailableByDefault() {
        assertFalse(oidcClientRegistrations.isAvailable());
    }

    @Test
    public void oauth2EndpointsAreNotActive() throws Exception {
        // not permitted and not mapped: an anonymous request is rejected, never sent to an IdP
        MvcResult result = mvc.perform(get("/oauth2/authorization/" + OidcSecurityConfig.REGISTRATION_ID))
                .andExpect(status().isUnauthorized()).andReturn();
        assertNull(result.getResponse().getRedirectedUrl());

        MvcResult callback = mvc.perform(get("/login/oauth2/code/" + OidcSecurityConfig.REGISTRATION_ID)
                .param("code", "x").param("state", "y")).andExpect(status().isUnauthorized()).andReturn();
        assertNull(callback.getResponse().getRedirectedUrl());
    }

    @Test
    public void loginPageHasNoSsoButton() throws Exception {
        String html = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("oidc-login"), html);
        assertFalse(html.contains("/oauth2/authorization"), html);
    }

    @Test
    public void advancedSettingsPageRendersOidcSectionWithoutSecret() throws Exception {
        MvcResult result = mvc.perform(get("/advancedSettings.view")
                .with(user("admin").roles("ADMIN", "USER", "SETTINGS")))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("id=\"oidcTable\""), "OIDC section must be rendered");
        assertTrue(html.contains("name=\"oidcEnabled\""));
        assertTrue(html.contains("name=\"oidcRequiredClaim\""));
        assertTrue(html.contains("/login/oauth2/code/airsonic"));
        assertFalse(html.matches("(?s).*name=\"oidcClientSecret\"[^>]*value=\"[^\"]+\".*"),
                "the client secret must not be echoed");
    }

    @Test
    public void formLoginIsUnchanged() throws Exception {
        mvc.perform(formLogin("/login").user("j_username", "admin").password("j_password", "admin"))
                .andExpect(authenticated().withUsername("admin"));
    }
}
