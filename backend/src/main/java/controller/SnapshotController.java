package com.investment.tracker.controller;

import com.investment.tracker.model.PortfolioSnapshot;
import com.investment.tracker.service.SnapshotGenerationService;
import com.investment.tracker.service.SnapshotGenerationService.MissingDataReport;
import com.investment.tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for managing historical portfolio snapshots.
 *
 * POST /api/snapshots/generate              – generate all missing FY-end snapshots
 * POST /api/snapshots/generate?force=true   – regenerate all (even existing)
 * POST /api/snapshots/recalculate?date=…    – recalculate a single snapshot
 * GET  /api/snapshots                       – list all snapshots for current user
 * GET  /api/snapshots/missing-data          – report on estimated/missing assets
 * DELETE /api/snapshots?date=…              – delete a specific snapshot
 */
@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
@Slf4j
public class SnapshotController {

    private final SnapshotGenerationService snapshotService;
    private final SecurityUtil securityUtil;

    /**
     * Generate historical snapshots for all FY-end dates since first investment.
     * Pass ?force=true to regenerate already-existing snapshots.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateSnapshots(
            @RequestParam(defaultValue = "false") boolean force) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("User {} requested snapshot generation (force={})", userId, force);

        List<PortfolioSnapshot> snapshots = snapshotService.generateHistoricalSnapshots(userId, force);

        return ResponseEntity.ok(Map.of(
                "message", "Snapshot generation complete",
                "generated", snapshots.size(),
                "snapshots", snapshots
        ));
    }

    /**
     * Recalculate (or create) the snapshot for a specific date.
     * Date must be in ISO format: yyyy-MM-dd (typically March 31 of a year).
     */
    @PostMapping("/recalculate")
    public ResponseEntity<PortfolioSnapshot> recalculateSnapshot(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("User {} requested snapshot recalculation for {}", userId, date);
        PortfolioSnapshot snapshot = snapshotService.generateSnapshotForDate(userId, date);
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Returns all snapshots for the current user (newest first).
     */
    @GetMapping
    public ResponseEntity<List<PortfolioSnapshot>> getSnapshots() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(snapshotService.getSnapshots(userId));
    }

    /**
     * Returns a report indicating how many snapshots have estimated/missing data.
     */
    @GetMapping("/missing-data")
    public ResponseEntity<MissingDataReport> getMissingDataReport() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(snapshotService.getMissingDataReport(userId));
    }

    /**
     * Deletes the snapshot for a specific date so it can be cleanly regenerated.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteSnapshot(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("User {} deleting snapshot for {}", userId, date);
        snapshotService.deleteSnapshot(userId, date);
        return ResponseEntity.noContent().build();
    }
}
