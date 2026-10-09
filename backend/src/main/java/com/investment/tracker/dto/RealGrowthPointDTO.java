package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * E13: one point of the inflation-adjusted (real) growth series.
 * Real values are always in INR, restated into today's rupees.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealGrowthPointDTO {
    private LocalDate date;
    /** Real (today's-rupee) value — the chart series. */
    private BigDecimal value;
    /** Nominal value in INR at that date (deflation input). */
    private BigDecimal nominalValue;
    /** CPI ratio applied: latest index ÷ index at this date's month. */
    private BigDecimal deflator;
    /** True when this date's month falls outside the official CPI table (projection used). */
    private boolean extrapolated;
    private String currency;
}
