package com.investment.tracker.service;

import com.investment.tracker.config.AppDefaults;
import com.investment.tracker.config.ExpenseImportRules;
import com.investment.tracker.dto.ExpenseImportReportDTO;
import com.investment.tracker.dto.ExpenseImportRowDTO;
import com.investment.tracker.dto.ExpenseRequest;
import com.investment.tracker.model.ExpenseCategory;
import com.investment.tracker.model.InvestmentType;
import com.investment.tracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Excel expense import pipeline (backlog feature F2):
 * parse → validate/infer → dry-run report → commit.
 *
 * <p>Balance/portfolio effect (E10): committed rows go through
 * {@code ExpenseService.createExpense}, which posts an EXPENSE transaction
 * against the user's cash account and refreshes the balance from the
 * ledger — so imported and manually entered expenses behave identically,
 * with a full transaction trail. Expense→asset coverage percentages
 * continue to flow through the existing mapping mechanism.</p>
 *
 * <p>The raw uploaded file is never persisted — rows are parsed in memory
 * and discarded.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseImportService {

    private final ExpenseExcelParser excelParser;
    private final ExpenseImportRules importRules;
    private final AppDefaults appDefaults;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;

    /**
     * Dry-run: parse and validate the sheet, returning a full row-by-row
     * report without persisting anything.
     */
    public ExpenseImportReportDTO preview(Long userId, MultipartFile file) throws IOException {
        return process(userId, file, false);
    }

    /**
     * Commit: same pipeline as preview, then persists every OK row and
     * applies the cash-balance impact.
     */
    @Transactional
    public ExpenseImportReportDTO commit(Long userId, MultipartFile file) throws IOException {
        return process(userId, file, true);
    }

    private ExpenseImportReportDTO process(Long userId, MultipartFile file, boolean commit)
            throws IOException {
        List<ExpenseExcelParser.RawRow> rawRows = excelParser.parse(file);
        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException(
                    "No data rows found in the sheet. Expected columns: "
                            + "Date | Description | Amount | Category | Currency | Notes");
        }

        Set<String> existingKeys = loadExistingExpenseKeys(userId);
        List<ExpenseImportRowDTO> results = new ArrayList<>();
        List<ExpenseImportRowDTO> committedRows = new ArrayList<>();

        for (ExpenseExcelParser.RawRow raw : rawRows) {
            ExpenseImportRowDTO row = validateRow(userId, raw, existingKeys);
            results.add(row);
            if (commit && ExpenseImportRowDTO.STATUS_OK.equals(row.getStatus())) {
                ExpenseRequest request = toRequest(row, raw);
                var saved = expenseService.createExpense(userId, request);
                row.setExpenseId(saved.getId());
                committedRows.add(row);
                // Register the key so duplicate rows within the same sheet are caught too.
                existingKeys.add(expenseKey(row.getExpenseDate(), row.getDescription(), row.getAmount()));
            }
        }

        if (commit && !committedRows.isEmpty()) {
            // E10: cash impact is applied per-row inside createExpense via the
            // cash ledger (EXPENSE transaction + balance replay) — nothing to
            // do here beyond logging.
            log.info("Import for user {}: {} expense(s) committed; cash ledger updated per row.",
                    userId, committedRows.size());
        }

        return buildReport(results, commit);
    }

    private ExpenseImportRowDTO validateRow(Long userId, ExpenseExcelParser.RawRow raw,
                                            Set<String> existingKeys) {
        ExpenseImportRowDTO.ExpenseImportRowDTOBuilder builder = ExpenseImportRowDTO.builder()
                .rowNumber(raw.rowNumber())
                .description(raw.description())
                .category(raw.category())
                .amount(raw.amount())
                .currency(raw.currency())
                .expenseDate(raw.date());

        if (raw.description() == null) {
            return builder.status(ExpenseImportRowDTO.STATUS_ERROR)
                    .message("Missing description").build();
        }
        if (raw.amount() == null || raw.amount().signum() <= 0) {
            return builder.status(ExpenseImportRowDTO.STATUS_ERROR)
                    .message("Missing or non-positive amount").build();
        }
        if (raw.date() == null) {
            return builder.status(ExpenseImportRowDTO.STATUS_ERROR)
                    .message("Missing or unparseable date (use YYYY-MM-DD or an Excel date cell)")
                    .build();
        }
        if (raw.date().isAfter(LocalDate.now().plusDays(1))) {
            return builder.status(ExpenseImportRowDTO.STATUS_ERROR)
                    .message("Expense date is in the future").build();
        }
        if (raw.category() != null) {
            try {
                ExpenseCategory.valueOf(raw.category().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return builder.status(ExpenseImportRowDTO.STATUS_ERROR)
                        .message("Unknown category '" + raw.category() + "'").build();
            }
        }
        String currency = raw.currency() != null
                ? raw.currency().trim().toUpperCase()
                : importRules.getDefaultCurrency();
        if (!appDefaults.getSupportedCurrencies().contains(currency)) {
            return builder.status(ExpenseImportRowDTO.STATUS_ERROR)
                    .message("Unsupported currency '" + currency + "'").build();
        }

        String category = raw.category() != null
                ? raw.category().trim().toUpperCase()
                : importRules.inferCategory(raw.description());

        String key = expenseKey(raw.date(), raw.description(), raw.amount());
        if (existingKeys.contains(key)) {
            return builder
                    .category(category)
                    .currency(currency)
                    .status(ExpenseImportRowDTO.STATUS_DUPLICATE)
                    .message("An expense with the same date, description and amount already exists")
                    .build();
        }

        return builder
                .category(category)
                .currency(currency)
                .status(ExpenseImportRowDTO.STATUS_OK)
                .build();
    }

    private ExpenseRequest toRequest(ExpenseImportRowDTO row, ExpenseExcelParser.RawRow raw) {
        return ExpenseRequest.builder()
                .description(row.getDescription())
                .category(ExpenseCategory.valueOf(row.getCategory()))
                .amount(row.getAmount())
                .currency(row.getCurrency())
                .expenseDate(row.getExpenseDate())
                .notes(raw.notes() != null ? raw.notes() : "Imported from Excel")
                .isRecurring(false)
                .build();
    }

    private Set<String> loadExistingExpenseKeys(Long userId) {
        Set<String> keys = new HashSet<>();
        for (var expense : expenseRepository.findByUserIdOrderByExpenseDateDesc(userId)) {
            keys.add(expenseKey(expense.getExpenseDate(), expense.getDescription(),
                    expense.getAmount()));
        }
        return keys;
    }

    private String expenseKey(LocalDate date, String description, BigDecimal amount) {
        return date + "|" + description.trim().toLowerCase() + "|" + amount.stripTrailingZeros();
    }

    private ExpenseImportReportDTO buildReport(List<ExpenseImportRowDTO> rows, boolean commit) {
        int valid = 0;
        int duplicates = 0;
        int errors = 0;
        for (ExpenseImportRowDTO row : rows) {
            if (ExpenseImportRowDTO.STATUS_OK.equals(row.getStatus())) {
                valid++;
            } else if (ExpenseImportRowDTO.STATUS_DUPLICATE.equals(row.getStatus())) {
                duplicates++;
            } else {
                errors++;
            }
        }
        return ExpenseImportReportDTO.builder()
                .dryRun(!commit)
                .totalRows(rows.size())
                .validRows(valid)
                .duplicateRows(duplicates)
                .errorRows(errors)
                .commitable(valid > 0)
                .rows(rows)
                .build();
    }
}
