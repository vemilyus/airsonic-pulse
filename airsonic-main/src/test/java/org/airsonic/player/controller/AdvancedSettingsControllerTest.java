package org.airsonic.player.controller;

import org.airsonic.player.command.AdvancedSettingsCommand;
import org.airsonic.player.service.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the OIDC part of {@link AdvancedSettingsController}.
 */
@ExtendWith(MockitoExtension.class)
class AdvancedSettingsControllerTest {

    @Mock
    private SettingsService settingsService;

    @InjectMocks
    private AdvancedSettingsController controller;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("music.example.com");
        request.setServerPort(443);
        request.setContextPath("/airsonic");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void populatesOidcFieldsWithoutTheClientSecret() {
        when(settingsService.isOidcEnabled()).thenReturn(true);
        when(settingsService.getOidcIssuerUri()).thenReturn("https://idp.example.com/realms/main");
        when(settingsService.getOidcClientId()).thenReturn("airsonic");
        when(settingsService.getOidcScopes()).thenReturn("openid email groups");
        when(settingsService.getOidcUsernameClaim()).thenReturn("email");
        when(settingsService.getOidcButtonLabel()).thenReturn("Keycloak");
        when(settingsService.isOidcAutoCreateUsers()).thenReturn(true);
        when(settingsService.isOidcLinkByEmail()).thenReturn(true);
        when(settingsService.getOidcRequiredClaim()).thenReturn("groups");
        when(settingsService.getOidcRequiredClaimValue()).thenReturn("airsonic-users");
        when(settingsService.getOidcGroupsClaim()).thenReturn("groups");
        when(settingsService.getOidcAdminGroup()).thenReturn("airsonic-admins");

        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("advancedSettings", controller.formBackingObject(model));

        AdvancedSettingsCommand command = (AdvancedSettingsCommand) model.get("command");
        assertTrue(command.isOidcEnabled());
        assertEquals("https://idp.example.com/realms/main", command.getOidcIssuerUri());
        assertEquals("airsonic", command.getOidcClientId());
        assertNull(command.getOidcClientSecret(), "the client secret must never be sent to the UI");
        verify(settingsService, never()).getOidcClientSecret();
        assertEquals("openid email groups", command.getOidcScopes());
        assertEquals("email", command.getOidcUsernameClaim());
        assertEquals("Keycloak", command.getOidcButtonLabel());
        assertTrue(command.isOidcAutoCreateUsers());
        assertTrue(command.isOidcLinkByEmail());
        assertEquals("groups", command.getOidcRequiredClaim());
        assertEquals("airsonic-users", command.getOidcRequiredClaimValue());
        assertEquals("groups", command.getOidcGroupsClaim());
        assertEquals("airsonic-admins", command.getOidcAdminGroup());
        assertEquals("https://music.example.com/airsonic/login/oauth2/code/airsonic", command.getOidcRedirectUri());
    }

    private AdvancedSettingsCommand validCommand() {
        AdvancedSettingsCommand command = new AdvancedSettingsCommand();
        command.setDownloadLimit("0");
        command.setUploadLimit("0");
        return command;
    }

    @Test
    void savesOidcFields() {
        AdvancedSettingsCommand command = validCommand();
        command.setOidcEnabled(true);
        command.setOidcIssuerUri("  https://idp.example.com/realms/main ");
        command.setOidcClientId(" airsonic ");
        command.setOidcClientSecret("new-secret");
        command.setOidcScopes("openid email groups");
        command.setOidcUsernameClaim("email");
        command.setOidcButtonLabel("Keycloak");
        command.setOidcAutoCreateUsers(true);
        command.setOidcLinkByEmail(true);
        command.setOidcRequiredClaim("realm_access.roles");
        command.setOidcRequiredClaimValue("airsonic-users");
        command.setOidcGroupsClaim("groups");
        command.setOidcAdminGroup("airsonic-admins");

        String view = controller.doSubmitAction(command, new RedirectAttributesModelMap());

        assertEquals("redirect:advancedSettings.view", view);
        verify(settingsService).setOidcEnabled(true);
        verify(settingsService).setOidcIssuerUri("https://idp.example.com/realms/main");
        verify(settingsService).setOidcClientId("airsonic");
        verify(settingsService).setOidcClientSecret("new-secret");
        verify(settingsService).setOidcScopes("openid email groups");
        verify(settingsService).setOidcUsernameClaim("email");
        verify(settingsService).setOidcButtonLabel("Keycloak");
        verify(settingsService).setOidcAutoCreateUsers(true);
        verify(settingsService).setOidcLinkByEmail(true);
        verify(settingsService).setOidcRequiredClaim("realm_access.roles");
        verify(settingsService).setOidcRequiredClaimValue("airsonic-users");
        verify(settingsService).setOidcGroupsClaim("groups");
        verify(settingsService).setOidcAdminGroup("airsonic-admins");
        verify(settingsService).save();
    }

    @Test
    void blankClientSecretKeepsThePreviousSecret() {
        AdvancedSettingsCommand command = validCommand();
        command.setOidcClientSecret("");

        controller.doSubmitAction(command, new RedirectAttributesModelMap());
        verify(settingsService, never()).setOidcClientSecret(any());

        command.setOidcClientSecret(null);
        controller.doSubmitAction(command, new RedirectAttributesModelMap());
        verify(settingsService, never()).setOidcClientSecret(any());
    }

    @Test
    void blankFieldsClearTheStoredValuesSoDefaultsApply() {
        AdvancedSettingsCommand command = validCommand();
        command.setOidcEnabled(false);
        command.setOidcIssuerUri("  ");
        command.setOidcScopes("");
        command.setOidcRequiredClaim(" ");
        command.setOidcRequiredClaimValue(null);

        controller.doSubmitAction(command, new RedirectAttributesModelMap());

        verify(settingsService).setOidcEnabled(false);
        verify(settingsService).setOidcIssuerUri(null);
        verify(settingsService).setOidcScopes(null);
        verify(settingsService).setOidcRequiredClaim(null);
        verify(settingsService).setOidcRequiredClaimValue(null);
        assertFalse(command.isOidcAutoCreateUsers());
    }
}
