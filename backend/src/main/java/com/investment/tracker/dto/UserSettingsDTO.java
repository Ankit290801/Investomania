package com.investment.tracker.dto;

import com.investment.tracker.model.UserSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read/write model for per-user settings (backlog feature F5).
 * Null fields on update mean "leave unchanged"; the effective
 * (default-filled) view is returned by every endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDTO {

    private Long userId;

    private String baseCurrency;

    private UserSettings.Theme theme;

    private UserSettings.DateFormat dateFormat;

    private String defaultMarket;

    private Integer marketDataRefreshSeconds;
}
