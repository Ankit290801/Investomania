package com.investment.tracker.controller;

import com.investment.tracker.dto.StatementImportReportDTO;
import com.investment.tracker.service.BankStatementImportService;
import com.investment.tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller for bank statement PDF import (backlog feature F6).
 */
@RestController
@RequestMapping("/api/statements/import")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:4173"})
public class StatementImportController {

    private final BankStatementImportService importService;
    private final SecurityUtil securityUtil;

    /**
     * POST /api/statements/import/preview — detect the bank, parse the
     * statement and return a row-by-row report. Nothing is persisted.
     * Optional {@code cashAccountId} overrides auto account matching.
     */
    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "cashAccountId", required = false)
                                     Long cashAccountId) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/statements/import/preview - User: {}, file: {} ({} bytes)",
                userId, file.getOriginalFilename(), file.getSize());
        try {
            StatementImportReportDTO report = cashAccountId != null
                    ? importService.preview(userId, file, cashAccountId)
                    : importService.preview(userId, file);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Statement preview failed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to read the PDF. Is it a valid bank statement?"));
        }
    }

    /**
     * POST /api/statements/import/commit — persist all new transactions,
     * replay the account balance and refresh FY-end snapshots retrospectively.
     */
    @PostMapping("/commit")
    public ResponseEntity<?> commit(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "cashAccountId", required = false)
                                    Long cashAccountId) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/statements/import/commit - User: {}, file: {} ({} bytes)",
                userId, file.getOriginalFilename(), file.getSize());
        try {
            return ResponseEntity.ok(importService.commit(userId, file, cashAccountId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Statement commit failed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process the PDF. Is it a valid bank statement?"));
        }
    }
}
