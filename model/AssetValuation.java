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
 * Stores the computed value of a single investment on a specific valuation date (March 31).
 * Provides per-asset breakdown for each PortfolioSnapshot.
 */
@Entity
@Table(name = "asset_valuations", indexes = {
    @Index(name = "idx_valuation_inv_date", columnList = "investment_id, valuation_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetValuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investment_id", nullable = false)
    private Long investmentId;

    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;

    @Column(name = "valuation_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal value;

    @Column(precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "price_per_unit", precision = 15, scale = 6)
    private BigDecimal pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValuationSource source;

    @Column(name = "source_details", length = 200)
    private String sourceDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
