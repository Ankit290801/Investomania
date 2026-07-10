package com.investment.tracker.dto;

import com.investment.tracker.model.Market;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Request DTO for updating/creating symbol mappings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolMappingRequest {
    
    @NotBlank(message = "User symbol is required")
    private String userSymbol;
    
    @NotBlank(message = "Yahoo symbol is required")
    private String yahooSymbol;
    
    @NotBlank(message = "Google symbol is required")
    private String googleSymbol;
    
    @NotNull(message = "Market is required")
    private Market market;
    
    private String name;
}
