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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing bond investments (government bonds, corporate bonds, debentures).
 */
@Entity
@DiscriminatorValue("BOND")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BondInvestment extends Investment {

    @NotBlank(message = "Issuer is required")
    @Column(length = 200)
    private String issuer; // Government, company name

    @Positive(message = "Face value must be positive")
    @Column(name = "face_value", precision = 15, scale = 2)
    private BigDecimal faceValue;

    @Positive(message = "Coupon rate must be positive")
    @Column(name = "coupon_rate", precision = 5, scale = 2)
    private BigDecimal couponRate; // Annual interest rate as percentage

    @NotNull(message = "Maturity date is required")
    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "credit_rating", length = 10)
    private String creditRating; // AAA, AA+, etc.
}
