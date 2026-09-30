package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for historical net worth data point.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalNetWorthDTO {
    private LocalDate date;
    private BigDecimal value;
    private String currency;
}
