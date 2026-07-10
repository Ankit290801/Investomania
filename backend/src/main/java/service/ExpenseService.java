package com.investment.tracker.service;

import com.investment.tracker.dto.*;
import com.investment.tracker.exception.ExpenseNotFoundException;
import com.investment.tracker.exception.InvalidMappingException;
import com.investment.tracker.exception.UnauthorizedAccessException;
import com.investment.tracker.model.Expense;
import com.investment.tracker.model.ExpenseAssetMapping;
import com.investment.tracker.model.ExpenseCategory;
import com.investment.tracker.model.Investment;
import com.investment.tracker.repository.ExpenseAssetMappingRepository;
import com.investment.tracker.repository.ExpenseRepository;
import com.investment.tracker.repository.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for expense management business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseAssetMappingRepository mappingRepository;
    private final InvestmentRepository investmentRepository;

    /**
     * Create a new expense.
     */
    public ExpenseDTO createExpense(Long userId, ExpenseRequest request) {
        log.info("Creating expense for user {}", userId);

        Expense expense = Expense.builder()
            .userId(userId)
            .description(request.getDescription())
            .category(request.getCategory())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .expenseDate(request.getExpenseDate())
            .notes(request.getNotes())
            .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
            .recurrenceFrequency(request.getRecurrenceFrequency())
            .build();

        Expense saved = expenseRepository.save(expense);
        return toDTO(saved);
    }

    /**
     * Update an existing expense.
     */
    public ExpenseDTO updateExpense(Long userId, Long expenseId, ExpenseRequest request) {
        log.info("Updating expense {} for user {}", expenseId, userId);

        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new ExpenseNotFoundException(expenseId));

        if (!expense.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to update this expense");
        }

        expense.setDescription(request.getDescription());
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setNotes(request.getNotes());
        expense.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        expense.setRecurrenceFrequency(request.getRecurrenceFrequency());

        Expense updated = expenseRepository.save(expense);
        return toDTO(updated);
    }

    /**
     * Get expense by ID.
     */
    @Transactional(readOnly = true)
    public ExpenseDTO getExpenseById(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new ExpenseNotFoundException(expenseId));

        if (!expense.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to view this expense");
        }

        return toDTOWithMappings(expense);
    }

    /**
     * Get all expenses for a user.
     */
    @Transactional(readOnly = true)
    public List<ExpenseDTO> getAllExpenses(Long userId) {
        log.info("Fetching all expenses for user {}", userId);
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(userId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get expenses by category.
     */
    @Transactional(readOnly = true)
    public List<ExpenseDTO> getExpensesByCategory(Long userId, ExpenseCategory category) {
        log.info("Fetching expenses for user {} and category {}", userId, category);
        return expenseRepository.findByUserIdAndCategoryOrderByExpenseDateDesc(userId, category)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get expenses by date range.
     */
    @Transactional(readOnly = true)
    public List<ExpenseDTO> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching expenses for user {} from {} to {}", userId, startDate, endDate);
        return expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Delete an expense.
     */
    public void deleteExpense(Long userId, Long expenseId) {
        log.info("Deleting expense {} for user {}", expenseId, userId);

        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new ExpenseNotFoundException(expenseId));

        if (!expense.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this expense");
        }

        // Delete all mappings first
        mappingRepository.deleteByExpenseId(expenseId);
        
        // Delete expense
        expenseRepository.delete(expense);
    }

    /**
     * Add expense-to-asset mapping.
     */
    public ExpenseAssetMappingDTO addMapping(Long userId, ExpenseAssetMappingRequest request) {
        log.info("Adding mapping for expense {} to investment {}", request.getExpenseId(), request.getInvestmentId());

        // Verify expense belongs to user
        Expense expense = expenseRepository.findById(request.getExpenseId())
            .orElseThrow(() -> new ExpenseNotFoundException(request.getExpenseId()));
        
        if (!expense.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to map this expense");
        }

        // Verify investment belongs to user
        Investment investment = investmentRepository.findByIdAndUserId(request.getInvestmentId(), userId)
            .orElseThrow(() -> new RuntimeException("Investment not found"));

        // Check total percentage doesn't exceed 100
        BigDecimal currentTotal = mappingRepository.sumPercentageByExpenseId(request.getExpenseId());
        BigDecimal newTotal = currentTotal.add(request.getPercentage());
        
        if (newTotal.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidMappingException("Total percentage cannot exceed 100%. Current: " + currentTotal + "%, Trying to add: " + request.getPercentage() + "%");
        }

        ExpenseAssetMapping mapping = ExpenseAssetMapping.builder()
            .expenseId(request.getExpenseId())
            .investmentId(request.getInvestmentId())
            .percentage(request.getPercentage())
            .notes(request.getNotes())
            .build();

        ExpenseAssetMapping saved = mappingRepository.save(mapping);
        return toMappingDTO(saved, investment.getName());
    }

    /**
     * Get mappings for an expense.
     */
    @Transactional(readOnly = true)
    public List<ExpenseAssetMappingDTO> getMappingsForExpense(Long userId, Long expenseId) {
        // Verify expense belongs to user
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
        
        if (!expense.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to view mappings");
        }

        return mappingRepository.findByExpenseId(expenseId)
            .stream()
            .map(mapping -> {
                Investment investment = investmentRepository.findById(mapping.getInvestmentId()).orElse(null);
                String investmentName = investment != null ? investment.getName() : "Unknown";
                return toMappingDTO(mapping, investmentName);
            })
            .collect(Collectors.toList());
    }

    /**
     * Delete a mapping.
     */
    public void deleteMapping(Long userId, Long mappingId) {
        ExpenseAssetMapping mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new RuntimeException("Mapping not found"));

        // Verify user owns the expense
        Expense expense = expenseRepository.findById(mapping.getExpenseId())
            .orElseThrow(() -> new ExpenseNotFoundException(mapping.getExpenseId()));
        
        if (!expense.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this mapping");
        }

        mappingRepository.delete(mapping);
    }

    /**
     * Convert Expense entity to DTO.
     */
    private ExpenseDTO toDTO(Expense expense) {
        BigDecimal mappedPercentage = mappingRepository.sumPercentageByExpenseId(expense.getId());
        
        return ExpenseDTO.builder()
            .id(expense.getId())
            .userId(expense.getUserId())
            .description(expense.getDescription())
            .category(expense.getCategory())
            .amount(expense.getAmount())
            .currency(expense.getCurrency())
            .expenseDate(expense.getExpenseDate())
            .notes(expense.getNotes())
            .isRecurring(expense.getIsRecurring())
            .recurrenceFrequency(expense.getRecurrenceFrequency())
            .createdAt(expense.getCreatedAt())
            .updatedAt(expense.getUpdatedAt())
            .mappedPercentage(mappedPercentage)
            .build();
    }

    /**
     * Convert Expense entity to DTO with mappings.
     */
    private ExpenseDTO toDTOWithMappings(Expense expense) {
        ExpenseDTO dto = toDTO(expense);
        List<ExpenseAssetMappingDTO> mappings = getMappingsForExpense(expense.getUserId(), expense.getId());
        dto.setMappings(mappings);
        return dto;
    }

    /**
     * Convert ExpenseAssetMapping entity to DTO.
     */
    private ExpenseAssetMappingDTO toMappingDTO(ExpenseAssetMapping mapping, String investmentName) {
        return ExpenseAssetMappingDTO.builder()
            .id(mapping.getId())
            .expenseId(mapping.getExpenseId())
            .investmentId(mapping.getInvestmentId())
            .investmentName(investmentName)
            .percentage(mapping.getPercentage())
            .notes(mapping.getNotes())
            .createdAt(mapping.getCreatedAt())
            .build();
    }
}
