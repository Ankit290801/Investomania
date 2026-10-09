package com.investment.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.investment.tracker.dto.BackupEnvelopeDTO;
import com.investment.tracker.model.User;
import com.investment.tracker.service.BackupService;
import com.investment.tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Phase 9 Task 8.4 — full-data JSON backup and restore endpoints.
 *
 * GET  /api/backup/export   → downloads investment-backup-YYYY-MM-DD.json
 * POST /api/backup/restore  → accepts the same format back (multipart file);
 *                             replaces the signed-in user's data atomically.
 */
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final BackupService backupService;
    private final SecurityUtil securityUtil;

    /**
     * GET /api/backup/export — download the signed-in user's full data as a
     * versioned JSON document.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportBackup() {
        User user = securityUtil.getCurrentUser();
        log.info("GET /api/backup/export - User: {}", user.getUsername());

        BackupEnvelopeDTO envelope = backupService.exportUserData(user.getId(), user.getUsername());

        try {
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            byte[] json = mapper.writeValueAsBytes(envelope);

            String filename = "investment-backup-"
                    + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".json";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        } catch (Exception e) {
            log.error("Backup serialisation failed for user {}", user.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("{\"error\": \"Backup serialisation failed: " + e.getMessage() + "\"}")
                            .getBytes());
        }
    }

    /**
     * POST /api/backup/restore — restore the signed-in user's data from a
     * previously exported JSON backup (multipart/form-data field "file").
     * Replaces ALL portfolio data for the user; this cannot be undone.
     */
    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> restoreBackup(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/backup/restore - User: {}, file: {} ({} bytes)",
                userId, file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No backup file uploaded."));
        }

        // ---- Stage 1: parse. Only a genuinely unreadable file lands here,
        // so the "not a valid backup JSON" message is now accurate. ----
        BackupEnvelopeDTO envelope;
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
            envelope = mapper.readValue(file.getInputStream(), BackupEnvelopeDTO.class);
        } catch (Exception e) {
            log.warn("Backup restore: unparseable file for user {}: {}", userId, e.toString());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Could not read the backup file. Is it a valid Investment Tracker backup JSON?"
            ));
        }

        // ---- Stage 2: restore. Integrity/validation failures must surface
        // their real messages — the earlier single catch-all masked the
        // actual cause (a snapshot unique-index violation) as a JSON-parsing
        // error and sent users hunting in the wrong direction. ----
        try {
            Map<String, Long> restored = backupService.restoreUserData(userId, envelope);
            return ResponseEntity.ok(Map.of(
                    "message", "Backup restored successfully.",
                    "restored", restored
            ));
        } catch (IllegalArgumentException e) {
            // Validation failures (version, structure, cross-references) — user error, clear message.
            log.warn("Backup restore rejected for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("Backup restore verification failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Backup restore failed for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Restore failed: " + e.getClass().getSimpleName()
                            + ". The server log has the full cause; your existing data is unchanged."
            ));
        }
    }
}
