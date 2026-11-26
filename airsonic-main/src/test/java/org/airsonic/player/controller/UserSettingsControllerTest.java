package org.airsonic.player.controller;

import org.airsonic.player.command.UserSettingsCommand;
import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.MediaFolderService;
import org.airsonic.player.service.PersonalSettingsService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.service.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Keppler
 * @version $Id$
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest
@ContextConfiguration(classes = {UserSettingsController.class}, initializers = {ConfigDataApplicationContextInitializer.class})
@EnableConfigurationProperties({AirsonicHomeConfig.class})
public class UserSettingsControllerTest {

    @MockitoBean
    private SecurityService securityService;
    @MockitoBean
    private SettingsService settingsService;
    @MockitoBean
    private MediaFolderService mediaFolderService;
    @MockitoBean
    private TranscodingService transcodingService;
    @MockitoBean
    private PersonalSettingsService personalSettingsService;
    @MockitoBean
    private UserService userService;

    @Autowired
    private UserSettingsController userSettingsController;

    @TempDir
    private static Path tempDir;

    @BeforeAll
    public static void setup() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @AfterAll
    public static void tearDown() {
        System.clearProperty("airsonic.home");
    }

    @Test
    void testUsersCanBeUpdated() {
        // Given I have a freshly created user
        final User newUser = new User("test", "test@example.com");

        // And I have a command for it
        final UserSettingsCommand command = new UserSettingsCommand();
        command.setNewUser(true);
        command.setUsername("test");
        command.setUser(newUser);
        command.setTranscodeSchemeName(TranscodeScheme.OFF.name());
        command.setLdapAuthenticated(false);
        command.setStreamRole(true);
        command.setSettingsRole(true);
        command.setAllowedMusicFolderIds(new int[] {1, 2, 3});

        when(securityService.updateUserByUserSettingsCommand(command)).thenReturn(newUser);
        final UserSettings someSettings = new UserSettings();
        when(personalSettingsService.getUserSettings("test")).thenReturn(someSettings);

        // When I run the update
        // Then the update succeeds
        assertDoesNotThrow(() -> userSettingsController.updateUser(command));
        verify(mediaFolderService).setMusicFoldersForUser(eq("test"), eq(Set.of(1, 2, 3)));
    }

    @Test
    void testUsersCanBeUpdatedWithoutAllowedMusicFolderIds() {
        // Given I have a freshly created user
        final User newUser = new User("test", "test@example.com");

        // And I have a command for it, that does not contain any "allowedMediaFolderIds"
        final UserSettingsCommand command = new UserSettingsCommand();
        command.setNewUser(true);
        command.setUsername("test");
        command.setUser(newUser);
        command.setTranscodeSchemeName(TranscodeScheme.OFF.name());
        command.setLdapAuthenticated(false);
        command.setStreamRole(true);
        command.setSettingsRole(true);

        // v~~~ Special case for when the system doesn't have any enabled or existing folders.
        command.setAllowedMusicFolderIds(null);
        when(securityService.updateUserByUserSettingsCommand(command)).thenReturn(newUser);
        final UserSettings someSettings = new UserSettings();
        when(personalSettingsService.getUserSettings("test")).thenReturn(someSettings);

        // When I run the update
        // Then the update succeeds
        assertDoesNotThrow(() -> userSettingsController.updateUser(command));
        verify(mediaFolderService).setMusicFoldersForUser(eq("test"), eq(Collections.emptySet()));
    }

    private UserSettingsCommand oidcCommand(User user) {
        final UserSettingsCommand command = new UserSettingsCommand();
        command.setUsername(user.getUsername());
        command.setUser(user);
        command.setTranscodeSchemeName(TranscodeScheme.OFF.name());
        command.setStreamRole(true);
        when(securityService.updateUserByUserSettingsCommand(command)).thenReturn(user);
        when(personalSettingsService.getUserSettings(user.getUsername())).thenReturn(new UserSettings());
        return command;
    }

    @Test
    void testAdminCanPreLinkOidcSubjectAtConfiguredIssuer() {
        final User user = new User("test", "test@example.com");
        final UserSettingsCommand command = oidcCommand(user);
        when(settingsService.isOidcConfigured()).thenReturn(true);
        when(settingsService.getOidcIssuerUri()).thenReturn(" https://idp.example.com/realms/main ");
        command.setOidcSubject(" subject-1 ");

        userSettingsController.updateUser(command);

        verify(securityService).createOidcCredential("test", "https://idp.example.com/realms/main", "subject-1",
                "OIDC link (pre-linked by admin)");
    }

    @Test
    void testNoPreLinkWithoutSubjectOrWhenOidcIsNotConfigured() {
        final User user = new User("test", "test@example.com");
        final UserSettingsCommand command = oidcCommand(user);
        when(settingsService.isOidcConfigured()).thenReturn(true);
        command.setOidcSubject("  ");
        userSettingsController.updateUser(command);

        when(settingsService.isOidcConfigured()).thenReturn(false);
        command.setOidcSubject("subject-1");
        userSettingsController.updateUser(command);

        verify(securityService, never()).createOidcCredential(any(), any(), any(), any());
    }

    @Test
    void testAdminCanRemoveOnlySelectedOidcLinksOfTheEditedUser() {
        final User user = new User("test", "test@example.com");
        final UserSettingsCommand command = oidcCommand(user);
        UserCredential keep = new UserCredential(user, "https://idp", "keep", "noop", App.OIDC);
        keep.setId(1);
        UserCredential drop = new UserCredential(user, "https://idp", "drop", "noop", App.OIDC);
        drop.setId(2);
        when(securityService.getCredentials("test", App.OIDC)).thenReturn(List.of(keep, drop));
        // id 99 belongs to somebody else and must be ignored
        command.setOidcRemoveIds(new Integer[] { 2, 2, 99 });

        userSettingsController.updateUser(command);

        verify(securityService).deleteCredential(drop);
        verify(securityService, never()).deleteCredential(keep);
    }
}
