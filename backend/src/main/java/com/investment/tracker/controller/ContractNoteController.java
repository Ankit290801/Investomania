package com.investment.tracker.controller;

import com.investment.tracker.dto.ContractNoteImportReportDTO;
import com.investment.tracker.service.ContractNoteImportService;
import com.investment.tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller for broker contract note (CNOTE) PDF import
 * (Phase 9 / Task 8.3).
 */
@RestController
@RequestMapping("/api/contract-notes/import")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:4173"})
public class ContractNoteController {

    private final ContractNoteImportService importService;
    private final SecurityUtil securityUtil;

    /**
     * POST /api/contract-notes/import/preview — detect broker, parse trades
     * and resolve the portfolio actions. Nothing is persisted.
     */
    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/contract-notes/import/preview - User: {}, file: {} ({} bytes)",
                userId, file.getOriginalFilename(), file.getSize());
        try {
            return ResponseEntity.ok(importService.preview(userId, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Contract note preview failed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to read the PDF. Is it a valid contract note?"));
        }
    }

    /**
     * POST /api/contract-notes/import/commit — apply all trades: equity
     * transactions, weighted-average holding updates / new investments,
     * settlement cash legs, retrospective snapshot refresh.
     */
    @PostMapping("/commit")
    public ResponseEntity<?> commit(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/contract-notes/import/commit - User: {}, file: {} ({} bytes)",
                userId, file.getOriginalFilename(), file.getSize());
        try {
            return ResponseEntity.ok(importService.commit(userId, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Contract note commit failed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process the PDF. Is it a valid contract note?"));
        }
    }
}
