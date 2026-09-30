package com.investment.tracker.dto;

import com.investment.tracker.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for transaction response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {

    private Long id;
    private Long investmentId;
    private String investmentName;
    private TransactionType type;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String currency;
    private String notes;
    private LocalDateTime createdAt;
}
