package org.airsonic.player.validator;

import org.airsonic.player.command.UserSettingsCommand;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserSettingsValidatorOidcTest {

    @Mock
    private SecurityService securityService;
    @Mock
    private SettingsService settingsService;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private UserSettingsValidator validator;
    private UserSettingsCommand command;

    @BeforeEach
    void setUp() {
        validator = new UserSettingsValidator(securityService, settingsService, request);
        when(securityService.getCurrentUser(request)).thenReturn(new User("admin", "admin@example.com"));
        command = new UserSettingsCommand();
        command.setUsername("alice");
        command.setAdminRole(true);
    }

    private Errors validate() {
        Errors errors = new BeanPropertyBindingResult(command, "command");
        validator.validate(command, errors);
        return errors;
    }

    @Test
    void noSubjectNoOidcErrors() {
        assertFalse(validate().hasFieldErrors("oidcSubject"));
        command.setOidcSubject("   ");
        assertFalse(validate().hasFieldErrors("oidcSubject"));
    }

    @Test
    void subjectRejectedWhenOidcIsNotConfigured() {
        when(settingsService.isOidcConfigured()).thenReturn(false);
        command.setOidcSubject("subject-1");

        Errors errors = validate();

        assertEquals("usersettings.oidcdisabled", errors.getFieldError("oidcSubject").getCode());
    }

    @Test
    void alreadyLinkedSubjectIsRejected() {
        when(settingsService.isOidcConfigured()).thenReturn(true);
        when(settingsService.getOidcIssuerUri()).thenReturn("https://idp.example.com");
        when(securityService.getOidcCredential("https://idp.example.com", "subject-1"))
                .thenReturn(Optional.of(mock(UserCredential.class)));
        command.setOidcSubject(" subject-1 ");

        Errors errors = validate();

        assertEquals("usersettings.oidcalreadylinked", errors.getFieldError("oidcSubject").getCode());
    }

    @Test
    void freeSubjectIsAccepted() {
        when(settingsService.isOidcConfigured()).thenReturn(true);
        when(settingsService.getOidcIssuerUri()).thenReturn("https://idp.example.com");
        when(securityService.getOidcCredential("https://idp.example.com", "subject-2")).thenReturn(Optional.empty());
        command.setOidcSubject("subject-2");

        assertNull(validate().getFieldError("oidcSubject"));
    }
}
