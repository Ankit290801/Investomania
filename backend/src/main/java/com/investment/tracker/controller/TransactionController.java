package com.investment.tracker.controller;

import com.investment.tracker.dto.TransactionCreateRequest;
import com.investment.tracker.dto.TransactionDTO;
import com.investment.tracker.exception.InvestmentNotFoundException;
import com.investment.tracker.service.TransactionService;
import com.investment.tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for transaction management endpoints.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final SecurityUtil securityUtil;

    /**
     * Add a new transaction to an investment.
     * @param investmentId the investment ID
     * @param request the transaction data
     * @return ResponseEntity with created transaction
     */
    @PostMapping("/investments/{investmentId}/transactions")
    public ResponseEntity<?> addTransaction(
            @PathVariable Long investmentId,
            @Valid @RequestBody TransactionCreateRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            TransactionDTO transaction = transactionService.addTransaction(userId, investmentId, request);
            log.info("Transaction {} added to investment {} for user {}", 
                    transaction.getType(), investmentId, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (InvestmentNotFoundException e) {
            log.error("Investment not found: {}", investmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get transaction history for an investment.
     * @param investmentId the investment ID
     * @return ResponseEntity with list of transactions
     */
    @GetMapping("/investments/{investmentId}/transactions")
    public ResponseEntity<?> getTransactionHistory(@PathVariable Long investmentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            List<TransactionDTO> transactions = transactionService.getTransactionHistory(userId, investmentId);
            log.info("Retrieved {} transactions for investment {}", transactions.size(), investmentId);
            return ResponseEntity.ok(transactions);
        } catch (InvestmentNotFoundException e) {
            log.error("Investment not found: {}", investmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving transaction history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all transactions for the authenticated user.
     * Optionally filter by date range.
     * @param startDate optional start date
     * @param endDate optional end date
     * @return ResponseEntity with list of transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            List<TransactionDTO> transactions;

            if (startDate != null && endDate != null) {
                transactions = transactionService.getTransactionsByDateRange(userId, startDate, endDate);
                log.info("Retrieved {} transactions for user {} between {} and {}", 
                        transactions.size(), userId, startDate, endDate);
            } else {
                transactions = transactionService.getAllTransactions(userId);
                log.info("Retrieved {} transactions for user {}", transactions.size(), userId);
            }

            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error retrieving transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete a transaction.
     * @param transactionId the transaction ID
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long transactionId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            transactionService.deleteTransaction(userId, transactionId);
            log.info("Transaction {} deleted for user {}", transactionId, userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Transaction deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
