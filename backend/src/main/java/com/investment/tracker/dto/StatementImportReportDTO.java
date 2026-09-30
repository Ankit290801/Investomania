package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of a bank statement PDF import (backlog feature F6):
 * dry-run preview before commit, commit report after.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementImportReportDTO {

    /** Bank detected from the statement markers; null when undetected. */
    private String detectedBank;

    /** True when this report is a dry-run (nothing persisted). */
    private boolean dryRun;

    private int totalRows;

    private int validRows;

    private int duplicateRows;

    private int errorRows;

    /** True when at least one row is commitable. */
    private boolean commitable;

    /** Balance impact on the matched CASH account once committed. */
    private BigDecimal netBalanceImpact;

    /** Cash account the statement was matched/created against. */
    private Long cashAccountId;

    private String cashAccountName;

    private List<StatementImportRowDTO> rows;

    /**
     * Per-row result of the statement import.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementImportRowDTO {

        private int rowNumber;

        private LocalDate date;

        private String description;

        private BigDecimal amount;

        /** CREDIT or DEBIT (direction on the statement). */
        private String direction;

        /** OK, DUPLICATE, or ERROR. */
        private String status;

        private String message;

        /** Set when the row committed successfully. */
        private Long transactionId;

        public static final String STATUS_OK = "OK";
        public static final String STATUS_DUPLICATE = "DUPLICATE";
        public static final String STATUS_ERROR = "ERROR";
    }
}
