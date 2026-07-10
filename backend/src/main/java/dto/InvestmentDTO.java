package com.investment.tracker.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.investment.tracker.model.InvestmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Base DTO for investment data transfer.
 * Uses JSON type info for polymorphic deserialization.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EquityInvestmentDTO.class, name = "EQUITY"),
    @JsonSubTypes.Type(value = BondInvestmentDTO.class, name = "BOND"),
    @JsonSubTypes.Type(value = FDInvestmentDTO.class, name = "FD"),
    @JsonSubTypes.Type(value = RDInvestmentDTO.class, name = "RD"),
    @JsonSubTypes.Type(value = NPSInvestmentDTO.class, name = "NPS"),
    @JsonSubTypes.Type(value = PPFInvestmentDTO.class, name = "PPF"),
    @JsonSubTypes.Type(value = RealEstateInvestmentDTO.class, name = "REAL_ESTATE"),
    @JsonSubTypes.Type(value = CryptoInvestmentDTO.class, name = "CRYPTO"),
    @JsonSubTypes.Type(value = CashInvestmentDTO.class, name = "CASH")
})
public abstract class InvestmentDTO {

    private Long id;

    @NotNull(message = "Investment type is required")
    private InvestmentType type;

    @NotBlank(message = "Investment name is required")
    private String name;

    @NotBlank(message = "Currency is required")
    private String currency;

    private BigDecimal currentValue;
    
    /** Date of the first BUY transaction for this investment. Null if no BUY transactions exist. */
    private LocalDate purchaseDate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
