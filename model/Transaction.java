package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a transaction related to an investment.
 * Tracks buy, sell, dividend, interest, and other investment activities.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_investment_id", columnList = "investment_id"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Investment ID is required")
    @Column(name = "investment_id", nullable = false)
    private Long investmentId;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(precision = 15, scale = 4)
    private BigDecimal quantity; // For equity/crypto transactions

    @Column(name = "price_per_unit", precision = 15, scale = 4)
    private BigDecimal pricePerUnit; // Price per share/unit

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount; // Total transaction amount

    @NotNull(message = "Transaction date is required")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotNull(message = "Currency is required")
    @Column(nullable = false, length = 3)
    private String currency; // ISO 4217 currency code

    @Column(length = 1000)
    private String notes; // Additional notes/comments

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
