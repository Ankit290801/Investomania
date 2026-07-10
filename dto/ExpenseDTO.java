package com.investment.tracker.dto;

import com.investment.tracker.model.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for expense response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDTO {

    private Long id;
    private Long userId;
    private String description;
    private ExpenseCategory category;
    private BigDecimal amount;
    private String currency;
    private LocalDate expenseDate;
    private String notes;
    private Boolean isRecurring;
    private String recurrenceFrequency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Optional: Include mappings if needed
    private List<ExpenseAssetMappingDTO> mappings;
    private BigDecimal mappedPercentage; // Total percentage mapped
}
