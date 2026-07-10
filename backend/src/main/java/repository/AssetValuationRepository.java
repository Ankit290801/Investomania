package com.investment.tracker.repository;

import com.investment.tracker.model.AssetValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetValuationRepository extends JpaRepository<AssetValuation, Long> {

    List<AssetValuation> findByInvestmentIdOrderByValuationDateAsc(Long investmentId);

    Optional<AssetValuation> findByInvestmentIdAndValuationDate(Long investmentId, LocalDate valuationDate);

    List<AssetValuation> findByValuationDate(LocalDate valuationDate);

    void deleteByInvestmentIdAndValuationDate(Long investmentId, LocalDate valuationDate);
}
