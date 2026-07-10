package com.investment.tracker.controller;

import com.investment.tracker.dto.*;
import com.investment.tracker.model.ExpenseCategory;
import com.investment.tracker.util.SecurityUtil;
import com.investment.tracker.service.ExpenseCoverageService;
import com.investment.tracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for expense management.
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:4173"})
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseCoverageService coverageService;
    private final SecurityUtil securityUtil;

    /**
     * POST /api/expenses
     * Create a new expense.
     */
    @PostMapping
    public ResponseEntity<ExpenseDTO> createExpense(@Valid @RequestBody ExpenseRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/expenses - User: {}", userId);
        
        ExpenseDTO expense = expenseService.createExpense(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    /**
     * PUT /api/expenses/{id}
     * Update an expense.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDTO> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        
        Long userId = securityUtil.getCurrentUserId();
        log.info("PUT /api/expenses/{} - User: {}", id, userId);
        
        ExpenseDTO expense = expenseService.updateExpense(userId, id, request);
        return ResponseEntity.ok(expense);
    }

    /**
     * GET /api/expenses/{id}
     * Get expense by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/expenses/{} - User: {}", id, userId);
        
        ExpenseDTO expense = expenseService.getExpenseById(userId, id);
        return ResponseEntity.ok(expense);
    }

    /**
     * GET /api/expenses
     * Get all expenses for current user.
     */
    @GetMapping
    public ResponseEntity<List<ExpenseDTO>> getAllExpenses(
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/expenses - User: {}, Category: {}, Dates: {} to {}", 
                userId, category, startDate, endDate);
        
        List<ExpenseDTO> expenses;
        
        if (startDate != null && endDate != null) {
            expenses = expenseService.getExpensesByDateRange(userId, startDate, endDate);
        } else if (category != null) {
            expenses = expenseService.getExpensesByCategory(userId, category);
        } else {
            expenses = expenseService.getAllExpenses(userId);
        }
        
        return ResponseEntity.ok(expenses);
    }

    /**
     * DELETE /api/expenses/{id}
     * Delete an expense.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("DELETE /api/expenses/{} - User: {}", id, userId);
        
        expenseService.deleteExpense(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/expenses/mappings
     * Add expense-to-asset mapping.
     */
    @PostMapping("/mappings")
    public ResponseEntity<ExpenseAssetMappingDTO> addMapping(
            @Valid @RequestBody ExpenseAssetMappingRequest request) {
        
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/expenses/mappings - User: {}", userId);
        
        ExpenseAssetMappingDTO mapping = expenseService.addMapping(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapping);
    }

    /**
     * GET /api/expenses/{expenseId}/mappings
     * Get mappings for an expense.
     */
    @GetMapping("/{expenseId}/mappings")
    public ResponseEntity<List<ExpenseAssetMappingDTO>> getMappings(@PathVariable Long expenseId) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/expenses/{}/mappings - User: {}", expenseId, userId);
        
        List<ExpenseAssetMappingDTO> mappings = expenseService.getMappingsForExpense(userId, expenseId);
        return ResponseEntity.ok(mappings);
    }

    /**
     * DELETE /api/expenses/mappings/{mappingId}
     * Delete a mapping.
     */
    @DeleteMapping("/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long mappingId) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("DELETE /api/expenses/mappings/{} - User: {}", mappingId, userId);
        
        expenseService.deleteMapping(userId, mappingId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/expenses/coverage
     * Get expense coverage metrics.
     */
    @GetMapping("/coverage")
    public ResponseEntity<ExpenseCoverageDTO> getCoverageMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "INR") String currency) {
        
        Long userId = securityUtil.getCurrentUserId();
        
        // Default to last 12 months if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        log.info("GET /api/expenses/coverage - User: {}, Dates: {} to {}", userId, startDate, endDate);
        
        ExpenseCoverageDTO coverage = coverageService.calculateCoverage(userId, startDate, endDate, currency);
        return ResponseEntity.ok(coverage);
    }

    /**
     * GET /api/expenses/category-breakdown
     * Get expense breakdown by category.
     */
    @GetMapping("/category-breakdown")
    public ResponseEntity<Map<String, BigDecimal>> getCategoryBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Long userId = securityUtil.getCurrentUserId();
        
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        log.info("GET /api/expenses/category-breakdown - User: {}", userId);
        
        Map<String, BigDecimal> breakdown = coverageService.getCategoryBreakdown(userId, startDate, endDate);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * GET /api/expenses/monthly-trend
     * Get monthly expense trend.
     */
    @GetMapping("/monthly-trend")
    public ResponseEntity<Map<String, BigDecimal>> getMonthlyTrend(
            @RequestParam(defaultValue = "12") int months) {
        
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/expenses/monthly-trend - User: {}, Months: {}", userId, months);
        
        Map<String, BigDecimal> trend = coverageService.getMonthlyTrend(userId, months);
        return ResponseEntity.ok(trend);
    }
}
