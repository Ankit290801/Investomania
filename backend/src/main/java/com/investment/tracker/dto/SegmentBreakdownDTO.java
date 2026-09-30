package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for segment breakdown response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentBreakdownDTO {
    private Map<String, BigDecimal> segments;
    private BigDecimal total;
    private String currency;
}
