package com.investment.tracker.service;

import com.investment.tracker.dto.TransactionCreateRequest;
import com.investment.tracker.dto.TransactionDTO;
import com.investment.tracker.exception.InvestmentNotFoundException;
import com.investment.tracker.model.*;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service layer for transaction management business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final InvestmentRepository investmentRepository;

    /**
     * Add a new transaction to an investment.
     * Also updates investment calculations (average price, quantity).
     */
    public TransactionDTO addTransaction(Long userId, Long investmentId, TransactionCreateRequest request) {
        // Verify investment exists and belongs to user
        Investment investment = investmentRepository.findByIdAndUserId(investmentId, userId)
                .orElseThrow(() -> new InvestmentNotFoundException(investmentId));

        // Create transaction
        Transaction transaction = Transaction.builder()
                .investmentId(investmentId)
                .type(request.getType())
                .quantity(request.getQuantity())
                .pricePerUnit(request.getPricePerUnit())
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .currency(request.getCurrency())
                .notes(request.getNotes())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction {} added to investment {} for user {}", 
                savedTransaction.getType(), investmentId, userId);

        // Update investment calculations if needed
        updateInvestmentCalculations(investment);

        return toDTO(savedTransaction, investment.getName());
    }

    /**
     * Get all transactions for an investment.
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionHistory(Long userId, Long investmentId) {
        // Verify investment belongs to user
        if (!investmentRepository.existsByIdAndUserId(investmentId, userId)) {
            throw new InvestmentNotFoundException(investmentId);
        }

        List<Transaction> transactions = transactionRepository
                .findByInvestmentIdOrderByTransactionDateDesc(investmentId);
        return toDTOs(transactions);
    }

    /**
     * Get transactions for a user within a date range.
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(userId, startDate, endDate);
        return toDTOs(transactions);
    }

    /**
     * Get all transactions for a user.
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactions(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        return toDTOs(transactions);
    }

    /**
     * Delete a transaction.
     */
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        // Verify the investment belongs to the user
        Investment investment = investmentRepository.findByIdAndUserId(transaction.getInvestmentId(), userId)
                .orElseThrow(() -> new InvestmentNotFoundException(transaction.getInvestmentId()));

        transactionRepository.deleteById(transactionId);
        log.info("Transaction {} deleted for user {}", transactionId, userId);

        // Update investment calculations
        updateInvestmentCalculations(investment);
    }

    /**
     * Update investment calculations based on transactions.
     * Calculates average price and current quantity for equity/crypto investments.
     */
    private void updateInvestmentCalculations(Investment investment) {
        if (investment instanceof EquityInvestment) {
            updateEquityCalculations((EquityInvestment) investment);
        } else if (investment instanceof CryptoInvestment) {
            updateCryptoCalculations((CryptoInvestment) investment);
        } else if (investment instanceof NPSInvestment) {
            updateNPSCalculations((NPSInvestment) investment);
        } else if (investment instanceof PPFInvestment) {
            updatePPFCalculations((PPFInvestment) investment);
        }
    }

    private void updateEquityCalculations(EquityInvestment equity) {
        List<Transaction> transactions = transactionRepository.findByInvestmentId(equity.getId());
        
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Transaction txn : transactions) {
            if (txn.getType() == TransactionType.BUY) {
                totalQuantity = totalQuantity.add(txn.getQuantity());
                totalAmount = totalAmount.add(txn.getAmount());
            } else if (txn.getType() == TransactionType.SELL) {
                totalQuantity = totalQuantity.subtract(txn.getQuantity());
            }
        }

        equity.setQuantity(totalQuantity);
        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avgPrice = totalAmount.divide(totalQuantity, 4, RoundingMode.HALF_UP);
            equity.setAvgPrice(avgPrice);
        }

        investmentRepository.save(equity);
        log.debug("Updated equity investment {}: quantity={}, avgPrice={}", 
                equity.getId(), equity.getQuantity(), equity.getAvgPrice());
    }

    private void updateCryptoCalculations(CryptoInvestment crypto) {
        List<Transaction> transactions = transactionRepository.findByInvestmentId(crypto.getId());
        
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Transaction txn : transactions) {
            if (txn.getType() == TransactionType.BUY) {
                totalQuantity = totalQuantity.add(txn.getQuantity());
                totalAmount = totalAmount.add(txn.getAmount());
            } else if (txn.getType() == TransactionType.SELL) {
                totalQuantity = totalQuantity.subtract(txn.getQuantity());
            }
        }

        crypto.setQuantity(totalQuantity);
        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avgPrice = totalAmount.divide(totalQuantity, 4, RoundingMode.HALF_UP);
            crypto.setAvgPrice(avgPrice);
        }

        investmentRepository.save(crypto);
        log.debug("Updated crypto investment {}: quantity={}, avgPrice={}", 
                crypto.getId(), crypto.getQuantity(), crypto.getAvgPrice());
    }

    private void updateNPSCalculations(NPSInvestment nps) {
        List<Transaction> transactions = transactionRepository.findByInvestmentId(nps.getId());
        
        BigDecimal totalContributed = transactions.stream()
                .filter(t -> t.getType() == TransactionType.CONTRIBUTION)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        nps.setTotalContributed(totalContributed);
        investmentRepository.save(nps);
        log.debug("Updated NPS investment {}: totalContributed={}", nps.getId(), totalContributed);
    }

    private void updatePPFCalculations(PPFInvestment ppf) {
        List<Transaction> transactions = transactionRepository.findByInvestmentId(ppf.getId());
        
        BigDecimal totalContributed = transactions.stream()
                .filter(t -> t.getType() == TransactionType.CONTRIBUTION)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ppf.setTotalContributed(totalContributed);
        investmentRepository.save(ppf);
        log.debug("Updated PPF investment {}: totalContributed={}", ppf.getId(), totalContributed);
    }

    /**
     * Convert Transaction entity to DTO, joining the linked investment's name.
     * Single-entity overload; performs a lookup. Prefer {@link #toDTOs(List)} for lists.
     */
    private TransactionDTO toDTO(Transaction transaction) {
        String investmentName = investmentRepository.findById(transaction.getInvestmentId())
                .map(Investment::getName)
                .orElse(null);
        return toDTO(transaction, investmentName);
    }

    private TransactionDTO toDTO(Transaction transaction, String investmentName) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .investmentId(transaction.getInvestmentId())
                .investmentName(investmentName)
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .pricePerUnit(transaction.getPricePerUnit())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .currency(transaction.getCurrency())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Batch-converts transactions to DTOs with one investment-name lookup per distinct
     * investment id, avoiding N+1 queries on list endpoints.
     */
    private List<TransactionDTO> toDTOs(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = transactions.stream()
                .map(Transaction::getInvestmentId)
                .collect(Collectors.toSet());
        Map<Long, String> nameById = new HashMap<>();
        investmentRepository.findAllById(ids)
                .forEach(inv -> nameById.put(inv.getId(), inv.getName()));
        return transactions.stream()
                .map(t -> toDTO(t, nameById.get(t.getInvestmentId())))
                .collect(Collectors.toList());
    }
}
