package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for expense-asset mapping request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAssetMappingRequest {

    @NotNull(message = "Expense ID is required")
    private Long expenseId;

    @NotNull(message = "Investment ID is required")
    private Long investmentId;

    @NotNull(message = "Percentage is required")
    @Min(value = 0, message = "Percentage must be at least 0")
    @Max(value = 100, message = "Percentage must not exceed 100")
    private BigDecimal percentage;

    private String notes;
}
