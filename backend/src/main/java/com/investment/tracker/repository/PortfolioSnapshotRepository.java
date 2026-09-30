package com.investment.tracker.repository;

import com.investment.tracker.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByUserIdOrderBySnapshotDateAsc(Long userId);

    List<PortfolioSnapshot> findByUserIdOrderBySnapshotDateDesc(Long userId);

    Optional<PortfolioSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    @Query("SELECT p FROM PortfolioSnapshot p WHERE p.userId = :userId ORDER BY p.snapshotDate DESC")
    List<PortfolioSnapshot> findTopByUserIdOrderBySnapshotDateDesc(
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    boolean existsByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    void deleteByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);
}
