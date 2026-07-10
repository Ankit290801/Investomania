package com.investment.tracker.repository;

import com.investment.tracker.model.PriceUpdateResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PriceUpdateResult entity
 */
@Repository
public interface PriceUpdateResultRepository extends JpaRepository<PriceUpdateResult, Long> {
    
    /**
     * Find all update results ordered by date descending
     */
    List<PriceUpdateResult> findAllByOrderByUpdateDateDesc();
    
    /**
     * Find update results within a date range
     */
    List<PriceUpdateResult> findByUpdateDateBetweenOrderByUpdateDateDesc(
        LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find the most recent update result
     */
    Optional<PriceUpdateResult> findFirstByOrderByUpdateDateDesc();
    
    /**
     * Find update results by trigger type
     */
    List<PriceUpdateResult> findByTriggerTypeOrderByUpdateDateDesc(
        PriceUpdateResult.TriggerType triggerType);
    
    /**
     * Find update results by status
     */
    List<PriceUpdateResult> findByStatusOrderByUpdateDateDesc(
        PriceUpdateResult.UpdateStatus status);
    
    /**
     * Get statistics for a date range
     */
    @Query("SELECT COUNT(pur), SUM(pur.successCount), SUM(pur.failureCount) " +
           "FROM PriceUpdateResult pur " +
           "WHERE pur.updateDate BETWEEN :startDate AND :endDate")
    Object[] getStatistics(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Delete old results (cleanup)
     */
    void deleteByUpdateDateBefore(LocalDateTime cutoffDate);
}
