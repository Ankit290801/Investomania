package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for expense coverage metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCoverageDTO {

    private BigDecimal totalExpenses;
    private BigDecimal totalIncome; // From investments (dividends, interest)
    private BigDecimal coveragePercentage;
    private Map<String, BigDecimal> categoryBreakdown; // Category -> Amount
    private Map<String, BigDecimal> monthlyTrend; // Month -> Amount
    private String currency;
}
