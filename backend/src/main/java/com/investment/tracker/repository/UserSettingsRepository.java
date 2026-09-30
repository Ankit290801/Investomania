package com.investment.tracker.repository;

import com.investment.tracker.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for UserSettings entity operations.
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    /**
     * The single settings row for a user.
     */
    Optional<UserSettings> findByUserId(Long userId);
}
