package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores the total portfolio value at each financial year end (March 31).
 * Used as the authoritative source for YoY growth calculations.
 */
@Entity
@Table(name = "portfolio_snapshots", indexes = {
    @Index(name = "idx_snapshot_user_date", columnList = "user_id, snapshot_date", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Financial year end date – always March 31 of a given year. */
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalValue;

    /** Base currency for all values in this snapshot (e.g. INR). */
    @Column(nullable = false, length = 3)
    private String currency;

    // ---- Asset-class breakdown ----
    @Column(name = "equity_value", precision = 18, scale = 2)
    private BigDecimal equityValue;

    @Column(name = "mutual_fund_value", precision = 18, scale = 2)
    private BigDecimal mutualFundValue;

    @Column(name = "fixed_deposit_value", precision = 18, scale = 2)
    private BigDecimal fixedDepositValue;

    @Column(name = "rd_value", precision = 18, scale = 2)
    private BigDecimal rdValue;

    @Column(name = "ppf_value", precision = 18, scale = 2)
    private BigDecimal ppfValue;

    @Column(name = "nps_value", precision = 18, scale = 2)
    private BigDecimal npsValue;

    @Column(name = "bond_value", precision = 18, scale = 2)
    private BigDecimal bondValue;

    @Column(name = "savings_value", precision = 18, scale = 2)
    private BigDecimal savingsValue;

    @Column(name = "crypto_value", precision = 18, scale = 2)
    private BigDecimal cryptoValue;

    @Column(name = "real_estate_value", precision = 18, scale = 2)
    private BigDecimal realEstateValue;

    @Column(name = "private_equity_value", precision = 18, scale = 2)
    private BigDecimal privateEquityValue;

    // ---- Metadata ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SnapshotStatus status;

    /** How many individual asset valuations were estimated (missing data). */
    @Column(name = "estimated_count")
    private Integer estimatedCount;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
}
