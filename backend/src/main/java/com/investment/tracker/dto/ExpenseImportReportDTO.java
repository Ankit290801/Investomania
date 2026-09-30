package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Overall result of an expense Excel import (backlog feature F2):
 * dry-run preview before commit, commit report after.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseImportReportDTO {

    /** True when this report is a dry-run (nothing persisted). */
    private boolean dryRun;

    private int totalRows;

    private int validRows;

    private int duplicateRows;

    private int errorRows;

    /** True when at least one row is commitable (valid or duplicate-skip). */
    private boolean commitable;

    private List<ExpenseImportRowDTO> rows;
}
