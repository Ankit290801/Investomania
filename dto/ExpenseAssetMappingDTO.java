package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for expense-asset mapping response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAssetMappingDTO {

    private Long id;
    private Long expenseId;
    private Long investmentId;
    private String investmentName; // For display
    private BigDecimal percentage;
    private String notes;
    private LocalDateTime createdAt;
}
