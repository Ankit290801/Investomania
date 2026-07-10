package com.investment.tracker.controller;

import com.investment.tracker.dto.InvestmentDTO;
import com.investment.tracker.exception.InvestmentNotFoundException;
import com.investment.tracker.model.InvestmentType;
import com.investment.tracker.service.InvestmentService;
import com.investment.tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for investment management endpoints.
 */
@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class InvestmentController {

    private final InvestmentService investmentService;
    private final SecurityUtil securityUtil;

    /**
     * Create a new investment.
     * @param investmentDTO the investment data
     * @return ResponseEntity with created investment
     */
    @PostMapping
    public ResponseEntity<?> createInvestment(@Valid @RequestBody InvestmentDTO investmentDTO) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            InvestmentDTO createdInvestment = investmentService.createInvestment(userId, investmentDTO);
            log.info("Investment created successfully for user {}: {}", userId, createdInvestment.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdInvestment);
        } catch (Exception e) {
            log.error("Error creating investment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all investments for the authenticated user.
     * @param type optional filter by investment type
     * @return ResponseEntity with list of investments
     */
    @GetMapping
    public ResponseEntity<?> getAllInvestments(@RequestParam(required = false) InvestmentType type) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            List<InvestmentDTO> investments;
            
            if (type != null) {
                investments = investmentService.getInvestmentsByType(userId, type);
                log.info("Retrieved {} investments of type {} for user {}", investments.size(), type, userId);
            } else {
                investments = investmentService.getAllInvestments(userId);
                log.info("Retrieved {} investments for user {}", investments.size(), userId);
            }
            
            return ResponseEntity.ok(investments);
        } catch (Exception e) {
            log.error("Error retrieving investments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get a specific investment by ID.
     * @param investmentId the investment ID
     * @return ResponseEntity with investment details
     */
    @GetMapping("/{investmentId}")
    public ResponseEntity<?> getInvestment(@PathVariable Long investmentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            InvestmentDTO investment = investmentService.getInvestment(userId, investmentId);
            return ResponseEntity.ok(investment);
        } catch (InvestmentNotFoundException e) {
            log.error("Investment not found: {}", investmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving investment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update an existing investment.
     * @param investmentId the investment ID
     * @param investmentDTO the updated investment data
     * @return ResponseEntity with updated investment
     */
    @PutMapping("/{investmentId}")
    public ResponseEntity<?> updateInvestment(
            @PathVariable Long investmentId,
            @Valid @RequestBody InvestmentDTO investmentDTO) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            InvestmentDTO updatedInvestment = investmentService.updateInvestment(userId, investmentId, investmentDTO);
            log.info("Investment {} updated successfully for user {}", investmentId, userId);
            return ResponseEntity.ok(updatedInvestment);
        } catch (InvestmentNotFoundException e) {
            log.error("Investment not found: {}", investmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating investment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete an investment.
     * @param investmentId the investment ID
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/{investmentId}")
    public ResponseEntity<?> deleteInvestment(@PathVariable Long investmentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            investmentService.deleteInvestment(userId, investmentId);
            log.info("Investment {} deleted successfully for user {}", investmentId, userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Investment deleted successfully");
            return ResponseEntity.ok(response);
        } catch (InvestmentNotFoundException e) {
            log.error("Investment not found: {}", investmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting investment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get investment count for the authenticated user.
     * @return ResponseEntity with investment count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getInvestmentCount() {
        try {
            Long userId = securityUtil.getCurrentUserId();
            long count = investmentService.getInvestmentCount(userId);
            
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting investment count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
