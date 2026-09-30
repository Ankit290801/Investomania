package com.investment.tracker.service;

import com.investment.tracker.config.AppDefaults;
import com.investment.tracker.dto.ChangePasswordRequest;
import com.investment.tracker.dto.UserSettingsDTO;
import com.investment.tracker.exception.InvalidCredentialsException;
import com.investment.tracker.exception.UnauthorizedAccessException;
import com.investment.tracker.model.User;
import com.investment.tracker.model.UserSettings;
import com.investment.tracker.repository.UserRepository;
import com.investment.tracker.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-user settings management (backlog feature F5 — Settings Page).
 *
 * Reads fill every unset field from the bundled application defaults so the
 * client always receives a complete, effective configuration; updates apply
 * partial patches over the stored row.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppDefaults appDefaults;

    /**
     * Returns the effective settings for a user, filling unset fields with
     * application defaults. Creates the row lazily on first access.
     */
    @Transactional
    public UserSettingsDTO getSettings(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserSettings created = UserSettings.builder()
                            .userId(userId)
                            .build();
                    return userSettingsRepository.save(created);
                });
        return toEffectiveDTO(settings);
    }

    /**
     * Applies a partial update: null DTO fields leave the stored value
     * unchanged. The updated effective settings are returned.
     */
    @Transactional
    public UserSettingsDTO updateSettings(Long userId, UserSettingsDTO update) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> userSettingsRepository.save(
                        UserSettings.builder().userId(userId).build()));

        if (update.getBaseCurrency() != null) {
            validateCurrency(update.getBaseCurrency());
            settings.setBaseCurrency(update.getBaseCurrency().toUpperCase());
        }
        if (update.getTheme() != null) {
            settings.setTheme(update.getTheme());
        }
        if (update.getDateFormat() != null) {
            settings.setDateFormat(update.getDateFormat());
        }
        if (update.getDefaultMarket() != null) {
            settings.setDefaultMarket(update.getDefaultMarket().trim());
        }
        if (update.getMarketDataRefreshSeconds() != null) {
            validateRefreshInterval(update.getMarketDataRefreshSeconds());
            settings.setMarketDataRefreshSeconds(update.getMarketDataRefreshSeconds());
        }

        UserSettings saved = userSettingsRepository.save(settings);
        log.info("Settings updated for user {}", userId);
        return toEffectiveDTO(saved);
    }

    /**
     * Changes the current user's password after verifying the current one.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        if (request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed for user {}: current password mismatch", userId);
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user {}", userId);
    }

    /**
     * Application-level option lists for rendering the settings UI.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("currencies", appDefaults.getSupportedCurrencies());
        options.put("defaultCurrency", appDefaults.getDefaultCurrency());
        options.put("themes", UserSettings.Theme.values());
        options.put("dateFormats", UserSettings.DateFormat.values());
        return options;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private UserSettingsDTO toEffectiveDTO(UserSettings settings) {
        return UserSettingsDTO.builder()
                .userId(settings.getUserId())
                .baseCurrency(settings.getBaseCurrency() != null
                        ? settings.getBaseCurrency()
                        : appDefaults.getDefaultCurrency())
                .theme(settings.getTheme() != null
                        ? settings.getTheme()
                        : UserSettings.Theme.SYSTEM)
                .dateFormat(settings.getDateFormat() != null
                        ? settings.getDateFormat()
                        : UserSettings.DateFormat.DD_MMM_YYYY)
                .defaultMarket(settings.getDefaultMarket() != null
                        ? settings.getDefaultMarket()
                        : "NSE")
                .marketDataRefreshSeconds(settings.getMarketDataRefreshSeconds() != null
                        ? settings.getMarketDataRefreshSeconds()
                        : 60)
                .build();
    }

    private void validateCurrency(String code) {
        if (!appDefaults.getSupportedCurrencies().contains(code.trim().toUpperCase())) {
            throw new IllegalArgumentException(
                    "Unsupported currency: " + code + ". Supported: "
                            + appDefaults.getSupportedCurrencies());
        }
    }

    private void validateRefreshInterval(Integer seconds) {
        if (seconds < 0 || seconds > 86400) {
            throw new IllegalArgumentException(
                    "Market data refresh interval must be between 0 and 86400 seconds");
        }
    }
}
