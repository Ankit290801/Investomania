package com.investment.tracker.service;

import com.investment.tracker.config.AppDefaults;
import com.investment.tracker.dto.ChangePasswordRequest;
import com.investment.tracker.dto.UserSettingsDTO;
import com.investment.tracker.exception.InvalidCredentialsException;
import com.investment.tracker.model.User;
import com.investment.tracker.model.UserSettings;
import com.investment.tracker.repository.UserRepository;
import com.investment.tracker.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for per-user settings (backlog feature F5).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AppDefaults appDefaults;

    private UserSettingsService settingsService;

    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        settingsService = new UserSettingsService(
                userSettingsRepository, userRepository, passwordEncoder, appDefaults);
        when(appDefaults.getDefaultCurrency()).thenReturn("INR");
        when(appDefaults.getSupportedCurrencies()).thenReturn(List.of("INR", "USD", "EUR", "GBP"));
        when(userSettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private UserSettings emptyRow() {
        return UserSettings.builder().userId(USER_ID).build();
    }

    @Test
    void getSettingsFillsApplicationDefaultsForUnsetFields() {
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emptyRow()));

        UserSettingsDTO effective = settingsService.getSettings(USER_ID);

        assertEquals("INR", effective.getBaseCurrency());
        assertEquals(UserSettings.Theme.SYSTEM, effective.getTheme());
        assertEquals(UserSettings.DateFormat.DD_MMM_YYYY, effective.getDateFormat());
        assertEquals("NSE", effective.getDefaultMarket());
        assertEquals(60, effective.getMarketDataRefreshSeconds());
    }

    @Test
    void getSettingsCreatesRowLazilyOnFirstAccess() {
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        settingsService.getSettings(USER_ID);

        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    void updateAppliesPartialPatchOverStoredValues() {
        UserSettings stored = emptyRow();
        stored.setBaseCurrency("USD");
        stored.setTheme(UserSettings.Theme.DARK);
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stored));

        UserSettingsDTO patch = new UserSettingsDTO();
        patch.setBaseCurrency("EUR"); // changed
        // everything else null → unchanged
        UserSettingsDTO effective = settingsService.updateSettings(USER_ID, patch);

        assertEquals("EUR", effective.getBaseCurrency());
        assertEquals(UserSettings.Theme.DARK, effective.getTheme(), "null patch fields must not reset stored values");
    }

    @Test
    void updateRejectsUnsupportedCurrency() {
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emptyRow()));

        UserSettingsDTO patch = new UserSettingsDTO();
        patch.setBaseCurrency("XYZ");

        assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSettings(USER_ID, patch));
    }

    @Test
    void updateRejectsInvalidRefreshInterval() {
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emptyRow()));

        UserSettingsDTO patch = new UserSettingsDTO();
        patch.setMarketDataRefreshSeconds(100000);

        assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSettings(USER_ID, patch));
    }

    @Test
    void changePasswordVerifiesCurrentPasswordAndEncodesNew() {
        User user = new User();
        user.setId(USER_ID);
        user.setPassword("hashed-current");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "hashed-current")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("hashed-new");

        settingsService.changePassword(USER_ID, new ChangePasswordRequest(
                "current", "new-password", "new-password"));

        assertEquals("hashed-new", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = new User();
        user.setId(USER_ID);
        user.setPassword("hashed-current");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-current")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> settingsService.changePassword(
                USER_ID, new ChangePasswordRequest("wrong", "new-password", "new-password")));
    }

    @Test
    void changePasswordRejectsMismatchedConfirmation() {
        assertThrows(IllegalArgumentException.class, () -> settingsService.changePassword(
                USER_ID, new ChangePasswordRequest("current", "new-password", "different")));
    }
}
