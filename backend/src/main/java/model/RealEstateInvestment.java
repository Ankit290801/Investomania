package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Entity representing Real Estate investments (residential, commercial property).
 */
@Entity
@DiscriminatorValue("REAL_ESTATE")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RealEstateInvestment extends Investment {

    @NotBlank(message = "Property type is required")
    @Column(name = "property_type", length = 50)
    private String propertyType; // RESIDENTIAL, COMMERCIAL, LAND

    @NotBlank(message = "Location is required")
    @Column(length = 500)
    private String location;

    @Positive(message = "Purchase price must be positive")
    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal purchasePrice;
}
