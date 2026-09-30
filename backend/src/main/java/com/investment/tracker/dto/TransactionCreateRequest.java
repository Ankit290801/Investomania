package com.investment.tracker.dto;

import com.investment.tracker.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreateRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    private BigDecimal quantity; // Optional, for equity/crypto transactions

    private BigDecimal pricePerUnit; // Optional, price per share/unit

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @NotNull(message = "Currency is required")
    private String currency;

    private String notes; // Optional notes
}
