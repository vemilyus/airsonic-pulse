package org.airsonic.player.service;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.config.AirsonicCueConfig;
import org.airsonic.player.config.AirsonicFolderConfig;
import org.airsonic.player.config.AirsonicHomeConfig;
import org.apache.commons.configuration2.spring.ConfigurationPropertySource;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test of the OIDC related parts of {@link SettingsService}.
 */
@SpringBootTest
public class OidcSettingsTest {

    @TempDir
    private static Path airsonicHome;

    private SettingsService settingsService;

    @Autowired
    private StandardEnvironment env;

    @Autowired
    private AirsonicFolderConfig defaultFolderConfig;

    @Autowired
    private AirsonicCueConfig cueConfig;

    @BeforeAll
    public static void setup() {
        System.setProperty("airsonic.home", airsonicHome.toString());
    }

    @BeforeEach
    public void setUpEach() throws IOException {
        TestCaseUtils.cleanAirsonicHomeForTest();
        FileUtils.deleteDirectory(airsonicHome.toFile());
        Files.createDirectories(airsonicHome);
        ConfigurationPropertiesService.reset();
        settingsService = newSettingsService();
    }

    private SettingsService newSettingsService() {
        SettingsService ss = new SettingsService();
        env.getPropertySources().addFirst(new ConfigurationPropertySource("airsonic-pre-init-configs",
                ConfigurationPropertiesService.getInstance().getConfiguration()));
        ss.setEnvironment(env);
        ss.setAirsonicConfig(new AirsonicHomeConfig(airsonicHome.toString(), null));
        ss.setAirsonicDefaultFolderConfig(defaultFolderConfig);
        ss.setAirsonicCueConfig(cueConfig);
        return ss;
    }

    @Test
    public void testSecureDefaults() {
        assertFalse(settingsService.isOidcEnabled());
        assertFalse(settingsService.isOidcConfigured());
        assertNull(settingsService.getOidcIssuerUri());
        assertNull(settingsService.getOidcClientId());
        assertNull(settingsService.getOidcClientSecret());
        assertEquals("openid profile email", settingsService.getOidcScopes());
        assertEquals("preferred_username", settingsService.getOidcUsernameClaim());
        assertEquals("", settingsService.getOidcGroupsClaim());
        assertEquals("", settingsService.getOidcAdminGroup());
        assertEquals("", settingsService.getOidcRequiredClaim());
        assertEquals("", settingsService.getOidcRequiredClaimValue());
        assertEquals("SSO", settingsService.getOidcButtonLabel());
        assertFalse(settingsService.isOidcAutoCreateUsers());
        assertFalse(settingsService.isOidcLinkByEmail());
    }

    @Test
    public void testRoundTrip() {
        settingsService.setOidcEnabled(true);
        settingsService.setOidcIssuerUri("https://idp.example.com/realms/test");
        settingsService.setOidcClientId("airsonic");
        settingsService.setOidcClientSecret("s3cr3t");
        settingsService.setOidcScopes("openid email groups");
        settingsService.setOidcUsernameClaim("email");
        settingsService.setOidcGroupsClaim("groups");
        settingsService.setOidcAdminGroup("airsonic-admins");
        settingsService.setOidcRequiredClaim("realm_access.roles");
        settingsService.setOidcRequiredClaimValue("airsonic-users");
        settingsService.setOidcButtonLabel("Keycloak");
        settingsService.setOidcAutoCreateUsers(true);
        settingsService.setOidcLinkByEmail(true);

        verify(settingsService);
        settingsService.save();
        verify(settingsService);
        verify(newSettingsService());
    }

    private void verify(SettingsService ss) {
        assertTrue(ss.isOidcEnabled());
        assertTrue(ss.isOidcConfigured());
        assertEquals("https://idp.example.com/realms/test", ss.getOidcIssuerUri());
        assertEquals("airsonic", ss.getOidcClientId());
        assertEquals("s3cr3t", ss.getOidcClientSecret());
        assertEquals("openid email groups", ss.getOidcScopes());
        assertEquals("email", ss.getOidcUsernameClaim());
        assertEquals("groups", ss.getOidcGroupsClaim());
        assertEquals("airsonic-admins", ss.getOidcAdminGroup());
        assertEquals("realm_access.roles", ss.getOidcRequiredClaim());
        assertEquals("airsonic-users", ss.getOidcRequiredClaimValue());
        assertEquals("Keycloak", ss.getOidcButtonLabel());
        assertTrue(ss.isOidcAutoCreateUsers());
        assertTrue(ss.isOidcLinkByEmail());
    }

    @Test
    public void testClientSecretIsStoredEncoded() {
        settingsService.setOidcClientSecret("s3cr3t");
        settingsService.save();

        Object stored = ConfigurationPropertiesService.getInstance().getConfiguration().getProperty("OidcClientSecret");
        assertNotEquals("s3cr3t", stored);
        assertEquals("s3cr3t", newSettingsService().getOidcClientSecret());

        settingsService.setOidcClientSecret(null);
        settingsService.save();
        assertNull(newSettingsService().getOidcClientSecret());
    }

    @Test
    public void testNotConfiguredWithoutIssuerOrClient() {
        settingsService.setOidcEnabled(true);
        assertFalse(settingsService.isOidcConfigured());

        settingsService.setOidcIssuerUri("https://idp.example.com");
        assertFalse(settingsService.isOidcConfigured());

        settingsService.setOidcClientId("airsonic");
        assertTrue(settingsService.isOidcConfigured());

        settingsService.setOidcEnabled(false);
        assertFalse(settingsService.isOidcConfigured());
    }

    @Test
    public void testEnvironmentOverridesFileValue() {
        settingsService.setOidcIssuerUri("https://from-file.example.com");
        settingsService.save();

        MapPropertySource override = new MapPropertySource("oidc-env-override", Map.of(
                "OidcIssuerUri", "https://from-env.example.com",
                "OidcEnabled", "true",
                "OidcRequiredClaim", "groups",
                "OidcRequiredClaimValue", "airsonic-users"));
        SettingsService fresh = newSettingsService();
        // the override must take precedence over the (later registered) properties file source
        env.getPropertySources().addFirst(override);
        try {
            assertEquals("https://from-env.example.com", fresh.getOidcIssuerUri());
            assertTrue(fresh.isOidcEnabled());
            assertEquals("groups", fresh.getOidcRequiredClaim());
            assertEquals("airsonic-users", fresh.getOidcRequiredClaimValue());
        } finally {
            env.getPropertySources().remove("oidc-env-override");
        }
    }
}
