package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for growth metrics response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthMetricsDTO {
    private BigDecimal yoyGrowth;
    private BigDecimal yoyGrowthPercentage;
    private BigDecimal cagr;
    private List<HistoricalNetWorthDTO> historicalData;
    private String currency;
}
