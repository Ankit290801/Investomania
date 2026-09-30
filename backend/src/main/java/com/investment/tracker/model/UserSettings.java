package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Per-user application preferences (backlog feature F5 — Settings Page).
 *
 * A row is created lazily on first settings read/update; every column falls
 * back to the bundled application defaults (currency-config.yml /
 * investment-rules.yml) when null, so a missing row is always a complete,
 * functional configuration (guidelines Section 2.4).
 */
@Entity
@Table(name = "user_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning user — one settings row per user. */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** Currency used for display aggregation (snapshots, net worth). */
    @Column(name = "base_currency", length = 3)
    private String baseCurrency;

    /** UI theme preference. */
    @Enumerated(EnumType.STRING)
    @Column(name = "theme", length = 10)
    private Theme theme;

    /** Preferred date display format token, e.g. DD_MMM_YYYY → "24 Sep 2026". */
    @Enumerated(EnumType.STRING)
    @Column(name = "date_format", length = 20)
    private DateFormat dateFormat;

    /** Default market preselected in equity forms (NSE/BSE/...). */
    @Column(name = "default_market", length = 20)
    private String defaultMarket;

    /** Market-data auto-refresh interval in seconds (0 = no auto-refresh). */
    @Column(name = "market_data_refresh_seconds")
    private Integer marketDataRefreshSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Theme {
        LIGHT, DARK, SYSTEM
    }

    public enum DateFormat {
        DD_MMM_YYYY,   // 24 Sep 2026
        DD_MM_YYYY,    // 24/09/2026
        YYYY_MM_DD     // 2026-09-24
    }
}
