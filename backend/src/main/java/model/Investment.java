package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Base entity for all investment types using SINGLE_TABLE inheritance strategy.
 * All investment-specific fields are stored in the same table with a discriminator column.
 */
@Entity
@Table(name = "investments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "investment_type", discriminatorType = DiscriminatorType.STRING)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Investment type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InvestmentType type;

    @NotBlank(message = "Investment name is required")
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank(message = "Currency is required")
    @Column(nullable = false, length = 3)
    private String currency; // ISO 4217 currency code (INR, USD, EUR, GBP)

    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue; // Calculated or manually updated

    @Column(name = "is_listed", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean isListed = true; // True for listed equities/crypto, false for private/unlisted

    /** Date the investment was first acquired. Authoritative source for historical valuation. */
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

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
}
