package com.investment.tracker.repository;

import com.investment.tracker.model.BondInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BondInvestment entity operations.
 */
@Repository
public interface BondInvestmentRepository extends JpaRepository<BondInvestment, Long> {

    /**
     * All bonds owned by a specific user.
     */
    List<BondInvestment> findByUserId(Long userId);

    /**
     * A single bond owned by a specific user (for authorization).
     */
    Optional<BondInvestment> findByIdAndUserId(Long id, Long userId);
}
