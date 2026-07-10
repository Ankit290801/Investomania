package com.investment.tracker.repository;

import com.investment.tracker.model.Investment;
import com.investment.tracker.model.InvestmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Investment entity operations.
 * Provides CRUD operations and custom queries for investment management.
 */
@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    /**
     * Find all investments for a specific user.
     */
    List<Investment> findByUserId(Long userId);

    /**
     * Find all investments for a specific user and investment type.
     */
    List<Investment> findByUserIdAndType(Long userId, InvestmentType type);

    /**
     * Find a specific investment by ID and user ID (for authorization).
     */
    Optional<Investment> findByIdAndUserId(Long id, Long userId);

    /**
     * Check if an investment exists for a user.
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Count total investments for a user.
     */
    long countByUserId(Long userId);
}
