package com.investment.tracker.controller;

import com.investment.tracker.dto.ChangePasswordRequest;
import com.investment.tracker.dto.UserSettingsDTO;
import com.investment.tracker.service.UserSettingsService;
import com.investment.tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * REST controller for per-user settings (backlog feature F5 — Settings Page).
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:4173"})
public class UserSettingsController {

    private final UserSettingsService userSettingsService;
    private final SecurityUtil securityUtil;

    /**
     * GET /api/settings — effective settings for the current user
     * (unset fields filled with application defaults).
     */
    @GetMapping
    public ResponseEntity<UserSettingsDTO> getSettings() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(userSettingsService.getSettings(userId));
    }

    /**
     * PUT /api/settings — partial update; null fields are left unchanged.
     */
    @PutMapping
    public ResponseEntity<UserSettingsDTO> updateSettings(@RequestBody UserSettingsDTO update) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("PUT /api/settings - User: {}", userId);
        return ResponseEntity.ok(userSettingsService.updateSettings(userId, update));
    }

    /**
     * GET /api/settings/options — option lists for rendering the UI
     * (supported currencies, themes, date formats).
     */
    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getOptions() {
        return ResponseEntity.ok(userSettingsService.getOptions());
    }

    /**
     * POST /api/settings/change-password — change the current user's password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/settings/change-password - User: {}", userId);
        userSettingsService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
