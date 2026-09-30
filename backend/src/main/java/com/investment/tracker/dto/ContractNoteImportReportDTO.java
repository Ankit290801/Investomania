package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of a contract note (CNOTE) import (Phase 9 / Task 8.3):
 * dry-run preview before commit, commit report after.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractNoteImportReportDTO {

    /** Broker detected from the document markers. */
    private String detectedBroker;

    /** True when this report is a dry-run (nothing persisted). */
    private boolean dryRun;

    private int totalTrades;

    private int validTrades;

    private int errorTrades;

    /** True when at least one trade is commitable. */
    private boolean commitable;

    /**
     * Net cash movement the note applies to the settlement account:
     * buys are outflows (negative), sells are inflows (positive).
     */
    private BigDecimal netCashImpact;

    /** Net portfolio value change the note applies (buys add cost, sells remove cost). */
    private BigDecimal netPortfolioImpact;

    /** Settlement cash account used/created for the cash legs. */
    private Long cashAccountId;

    private String cashAccountName;

    private List<ContractNoteTradeDTO> trades;

    /**
     * Per-trade result of the contract note import.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractNoteTradeDTO {

        private int rowNumber;

        private String symbol;

        private boolean buy;

        private BigDecimal quantity;

        private BigDecimal price;

        private LocalDate tradeDate;

        /** NEW, UPDATED (existing holding), or ERROR. */
        private String status;

        /** Human-readable description of what will happen / what happened. */
        private String message;

        /** Portfolio action taken/applied: CREATE_INVESTMENT, UPDATE_HOLDING, ERROR. */
        private String action;

        /** Set when the trade committed (investment id affected). */
        private Long investmentId;

        /** Set on commit: the settlement TRANSFER_OUT/TRANSFER_IN transaction id. */
        private Long cashTransactionId;

        public static final String STATUS_NEW = "NEW";
        public static final String STATUS_UPDATED = "UPDATED";
        public static final String STATUS_ERROR = "ERROR";

        public static final String ACTION_CREATE = "CREATE_INVESTMENT";
        public static final String ACTION_UPDATE = "UPDATE_HOLDING";
        public static final String ACTION_ERROR = "ERROR";
    }
}
