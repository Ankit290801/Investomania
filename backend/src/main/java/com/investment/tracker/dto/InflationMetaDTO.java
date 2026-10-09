package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * E13: metadata about the CPI series behind the real-growth figures, so the
 * UI can caption the source and the labelled extrapolation assumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InflationMetaDTO {
    /** Human-readable series source, e.g. "All-India monthly CPI (OECD MEI / MoSPI, index 2015=100)". */
    private String source;
    /** Known-accuracy disclosure for the series. */
    private String seriesNote;
    /** First month of the official table, e.g. "2012-01". */
    private String tableStart;
    /** Last month with an official index value, e.g. "2023-12". */
    private String tableEnd;
    /** Number of official monthly rows currently loaded. */
    private Integer monthCount;
    /** Assumed annual inflation rate beyond the table (%), e.g. 4.00. */
    private BigDecimal assumedAnnualRatePct;
    /** True when the in-memory table was replaced by a successful API refresh. */
    private Boolean refreshedFromApi;
    /** True when the current month needs the extrapolated tail. */
    private Boolean extrapolatesCurrentData;
}
