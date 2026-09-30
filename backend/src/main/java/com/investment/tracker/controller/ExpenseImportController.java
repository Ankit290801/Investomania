package com.investment.tracker.controller;

import com.investment.tracker.dto.ExpenseImportReportDTO;
import com.investment.tracker.service.ExpenseImportService;
import com.investment.tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller for the Excel expense import (backlog feature F2).
 */
@RestController
@RequestMapping("/api/expenses/import")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:4173"})
public class ExpenseImportController {

    private final ExpenseImportService expenseImportService;
    private final SecurityUtil securityUtil;

    /**
     * POST /api/expenses/import/preview — parse + validate an uploaded Excel
     * sheet and return a row-by-row report. Nothing is persisted.
     */
    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/expenses/import/preview - User: {}, file: {} ({} bytes)",
                userId, file.getOriginalFilename(), file.getSize());
        try {
            return ResponseEntity.ok(expenseImportService.preview(userId, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Import preview failed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to read the file. Is it a valid .xlsx workbook?"));
        }
    }

    /**
     * POST /api/expenses/import/commit — parse, validate, persist all valid
     * rows and apply the cash-balance impact. Returns the commit report.
     */
    @PostMapping("/commit")
    public ResponseEntity<?> commit(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/expenses/import/commit - User: {}, file: {} ({} bytes)",
                userId, file.getOriginalFilename(), file.getSize());
        try {
            return ResponseEntity.ok(expenseImportService.commit(userId, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Import commit failed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process the file. Is it a valid .xlsx workbook?"));
        }
    }
}
