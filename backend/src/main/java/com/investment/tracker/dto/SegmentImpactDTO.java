package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for segment impact analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentImpactDTO {
    private Map<String, BigDecimal> segmentContributions;
    private Map<String, BigDecimal> segmentReturns;
    private String bestPerformingSegment;
    private String worstPerformingSegment;
    private String currency;
}
