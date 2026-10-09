package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * E13: inflation-adjusted portfolio growth, per fiscal.
 * Real figures are always computed and returned in INR (agreed scope,
 * October 3 2026) regardless of the dashboard currency selector.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealGrowthMetricsDTO {
    /** Deflated growth series (today's rupees) for the real chart toggle. */
    private List<RealGrowthPointDTO> realSeries;
    /** Per-fiscal-year breakdown (nominal, real, inflation, returns, XIRR). */
    private List<FiscalRealBreakdownDTO> fiscalYears;
    /** CPI series metadata for UI captions. */
    private InflationMetaDTO inflation;
    private String currency;
}
