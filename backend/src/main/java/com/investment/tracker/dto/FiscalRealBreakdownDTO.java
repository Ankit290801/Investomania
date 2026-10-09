package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * E13: per-fiscal-year real growth breakdown (Indian FY ends 31 March).
 * Percent fields are already ×100 (e.g. 5.79 means +5.79%); null when the
 * prior fiscal year has no comparable data point.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalRealBreakdownDTO {
    /** e.g. "FY 22-23" (same convention as the frontend toFYLabel). */
    private String fyLabel;
    /** Date of the FY-end representative portfolio value (last point ≤ 31 March). */
    private LocalDate fyEndDate;
    /** Nominal FY-end portfolio value in INR. */
    private BigDecimal nominalValue;
    /** FY-end portfolio value restated in today's rupees (INR). */
    private BigDecimal realValue;
    /** Mar-over-Mar CPI inflation across this FY (%). */
    private BigDecimal inflationPct;
    /** Nominal value change vs the previous FY-end (%). */
    private BigDecimal nominalReturnPct;
    /** Real (purchasing-power) value change vs the previous FY-end (%). */
    private BigDecimal realReturnPct;
    /** True when the FY contained at least one realized cash-flow transaction. */
    private Boolean hasCashFlows;
    /** Realized FY XIRR, nominal (annualized %). */
    private BigDecimal xirrNominalPct;
    /** Realized FY XIRR with every flow deflated to today's rupees (annualized %). */
    private BigDecimal xirrRealPct;
}
