package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO for Real Estate investment data.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RealEstateInvestmentDTO extends InvestmentDTO {

    @NotBlank(message = "Property type is required")
    private String propertyType;

    @NotBlank(message = "Location is required")
    private String location;

    @Positive(message = "Purchase price must be positive")
    private BigDecimal purchasePrice;
}
