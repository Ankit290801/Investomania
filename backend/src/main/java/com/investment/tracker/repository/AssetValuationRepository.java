package com.investment.tracker.repository;

import com.investment.tracker.model.AssetValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetValuationRepository extends JpaRepository<AssetValuation, Long> {

    List<AssetValuation> findByInvestmentIdOrderByValuationDateAsc(Long investmentId);

    /** Task 8.4 backup/restore: clear valuations for one investment. */
    void deleteByInvestmentId(Long investmentId);

    /**
     * Task 8.4 backup/restore: bulk delete of every valuation hanging off the
     * user's investments. Immediate JPQL execution (flush-order safe) — see
     * TransactionRepository.deleteAllForUser.
     */
    @Modifying
    @Query("DELETE FROM AssetValuation v WHERE v.investmentId IN " +
           "(SELECT i.id FROM Investment i WHERE i.userId = :userId)")
    void deleteAllForUser(@Param("userId") Long userId);

    Optional<AssetValuation> findByInvestmentIdAndValuationDate(Long investmentId, LocalDate valuationDate);

    List<AssetValuation> findByValuationDate(LocalDate valuationDate);

    void deleteByInvestmentIdAndValuationDate(Long investmentId, LocalDate valuationDate);

    /**
     * Bulk delete of one (investment, date) valuation — immediate JPQL
     * execution, flush-order safe (same rationale as
     * PortfolioSnapshotRepository.deleteByUserIdAndSnapshotDateBulk: a deferred
     * entity delete followed by save() re-inserts before the DELETE executes
     * and violates the unique constraint).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AssetValuation v WHERE v.investmentId = :investmentId AND v.valuationDate = :date")
    int deleteByInvestmentIdAndValuationDateBulk(@Param("investmentId") Long investmentId, @Param("date") LocalDate date);
}
