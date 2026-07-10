package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track the history and results of scheduled price update jobs.
 * Provides audit trail for automated market data updates.
 */
@Entity
@Table(name = "price_update_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "total_symbols", nullable = false)
    private Integer totalSymbols;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs; // Duration in milliseconds

    @Column(name = "failed_symbols", columnDefinition = "TEXT")
    private String failedSymbols; // Comma-separated list of failed symbols

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TriggerType triggerType; // SCHEDULED, MANUAL

    @Column(name = "triggered_by")
    private String triggeredBy; // Username if manual, "SYSTEM" if scheduled

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Overall error message if job failed

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UpdateStatus status; // SUCCESS, PARTIAL_SUCCESS, FAILURE

    /**
     * Enum for trigger type
     */
    public enum TriggerType {
        SCHEDULED, // Triggered by scheduler
        MANUAL     // Triggered by user
    }

    /**
     * Enum for update status
     */
    public enum UpdateStatus {
        SUCCESS,         // All symbols updated successfully
        PARTIAL_SUCCESS, // Some symbols failed
        FAILURE          // All symbols failed or job crashed
    }

    /**
     * Calculate success rate as percentage
     */
    public double getSuccessRate() {
        if (totalSymbols == 0) return 0.0;
        return (successCount * 100.0) / totalSymbols;
    }

    /**
     * Check if update was completely successful
     */
    public boolean isCompleteSuccess() {
        return status == UpdateStatus.SUCCESS && failureCount == 0;
    }

    @PrePersist
    protected void onCreate() {
        if (updateDate == null) {
            updateDate = LocalDateTime.now();
        }
    }
}
