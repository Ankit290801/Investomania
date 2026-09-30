package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-row result of an expense Excel import (backlog feature F2).
 * Part of the dry-run preview and the commit report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseImportRowDTO {

    /** 1-based row number in the spreadsheet (header excluded). */
    private int rowNumber;

    private String description;

    private String category;

    private BigDecimal amount;

    private String currency;

    private LocalDate expenseDate;

    /** OK, DUPLICATE, or ERROR. */
    private String status;

    /** Human-readable validation message for ERROR/DUPLICATE rows. */
    private String message;

    /** Set when the row committed successfully (commit phase only). */
    private Long expenseId;

    public static final String STATUS_OK = "OK";
    public static final String STATUS_DUPLICATE = "DUPLICATE";
    public static final String STATUS_ERROR = "ERROR";
}
