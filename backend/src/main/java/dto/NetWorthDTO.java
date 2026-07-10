package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for net worth response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetWorthDTO {
    private BigDecimal totalValue;
    private String currency;
    private BigDecimal growth;
    private BigDecimal growthPercentage;
}
