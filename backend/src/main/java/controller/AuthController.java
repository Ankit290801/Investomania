package com.investment.tracker.controller;

import com.investment.tracker.dto.LoginRequest;
import com.investment.tracker.dto.LoginResponse;
import com.investment.tracker.dto.RegisterRequest;
import com.investment.tracker.exception.InvalidCredentialsException;
import com.investment.tracker.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint for user authentication.
     * @param loginRequest the login credentials
     * @return ResponseEntity with user information on success
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (InvalidCredentialsException e) {
            log.error("Login failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Register endpoint for new user registration.
     * @param registerRequest the registration details
     * @return ResponseEntity with user information on success
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            LoginResponse response = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Registration failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Logout endpoint.
     * @return ResponseEntity with success message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Session invalidation is handled by Spring Security
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user information.
     * @return ResponseEntity with current user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        // This endpoint can be used to check if user is authenticated
        // and retrieve current user information from SecurityContext
        Map<String, String> response = new HashMap<>();
        response.put("message", "User is authenticated");
        return ResponseEntity.ok(response);
    }
}
