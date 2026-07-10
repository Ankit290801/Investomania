package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing the mapping between an expense and investment(s).
 * Allows tracking which investments cover which expenses and by what percentage.
 */
@Entity
@Table(name = "expense_asset_mappings", indexes = {
    @Index(name = "idx_expense_mapping_expense_id", columnList = "expense_id"),
    @Index(name = "idx_expense_mapping_investment_id", columnList = "investment_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAssetMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Expense ID is required")
    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @NotNull(message = "Investment ID is required")
    @Column(name = "investment_id", nullable = false)
    private Long investmentId;

    @NotNull(message = "Percentage is required")
    @Min(value = 0, message = "Percentage must be at least 0")
    @Max(value = 100, message = "Percentage must not exceed 100")
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal percentage; // Percentage of expense covered by this investment

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
