package com.investment.tracker.service;

import com.investment.tracker.model.CashInvestment;
import com.investment.tracker.model.Investment;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * E10 — single owner of cash-account ledger semantics.
 *
 * <p>A cash account's balance is the replay of its transaction history: the
 * opening DEPOSIT/BUY seeds it and every recognised flow adjusts it. All
 * replay sites (historical valuation, snapshots, statement import, interest
 * calculation) delegate here so a new flow type (e.g. EXPENSE) is honoured
 * everywhere at once.</p>
 *
 * <p>Also posts and reverses EXPENSE transactions on behalf of the expense
 * lifecycle (create/update/delete) and the one-off backfill.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CashLedgerService {

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;

    // ------------------------------------------------------------------
    // Ledger semantics (the one replay implementation)
    // ------------------------------------------------------------------

    /** True if this transaction type adds to a cash balance. */
    public boolean isCreditFlow(TransactionType type) {
        return type == TransactionType.DEPOSIT
                || type == TransactionType.TRANSFER_IN
                || type == TransactionType.SALARY
                || type == TransactionType.INTEREST;
    }

    /** True if this transaction type subtracts from a cash balance. */
    public boolean isDebitFlow(TransactionType type) {
        return type == TransactionType.TRANSFER_OUT
                || type == TransactionType.WITHDRAWAL
                || type == TransactionType.EXPENSE;
    }

    /**
     * Replay the account balance as of {@code asOfDate} (inclusive) from the
     * full transaction history. The opening DEPOSIT/BUY seeds the balance;
     * flows after it adjust it. Falls back to the stored {@code currentValue}
     * when the account has no transactions.
     */
    public BigDecimal replayBalance(CashInvestment account, LocalDate asOfDate) {
        List<Transaction> txns =
                transactionRepository.findByInvestmentIdOrderByTransactionDateDesc(account.getId());
        if (txns == null || txns.isEmpty()) {
            return account.getCurrentValue() != null ? account.getCurrentValue() : BigDecimal.ZERO;
        }

        List<Transaction> chronological = new ArrayList<>(txns);
        java.util.Collections.reverse(chronological); // oldest first

        BigDecimal balance = BigDecimal.ZERO;
        boolean opened = false;
        for (Transaction txn : chronological) {
            if (txn.getTransactionDate().isAfter(asOfDate)) {
                continue; // future-dated relative to the valuation point
            }
            if (!opened && (txn.getType() == TransactionType.DEPOSIT
                    || txn.getType() == TransactionType.BUY)) {
                balance = txn.getAmount();
                opened = true;
                continue;
            }
            opened = true;
            if (isCreditFlow(txn.getType())) {
                balance = balance.add(txn.getAmount());
            } else if (isDebitFlow(txn.getType())) {
                balance = balance.subtract(txn.getAmount());
            }
        }
        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    /** Convenience: replay up to today and persist as the current balance. */
    @Transactional
    public void refreshCurrentBalance(CashInvestment account) {
        BigDecimal balance = replayBalance(account, LocalDate.now());
        account.setCurrentValue(balance);
        investmentRepository.save(account);
        log.debug("Cash account {} balance refreshed to {} {}.",
                account.getId(), account.getCurrentValue(), account.getCurrency());
    }

    // ------------------------------------------------------------------
    // Cash-account resolution for expense linkage
    // ------------------------------------------------------------------

    /**
     * Resolve the cash account an expense draws from: the explicitly chosen
     * one when valid, else the user's only cash account when exactly one
     * exists, else nothing (no linkage; balance untouched).
     */
    @Transactional(readOnly = true)
    public Optional<CashInvestment> resolveCashAccount(Long userId, Long paidFromInvestmentId) {
        if (paidFromInvestmentId != null) {
            Investment inv = investmentRepository.findByIdAndUserId(paidFromInvestmentId, userId)
                    .orElse(null);
            if (inv instanceof CashInvestment) {
                return Optional.of((CashInvestment) inv);
            }
            log.warn("paidFromInvestmentId {} for user {} is not a CASH investment — ignoring.",
                    paidFromInvestmentId, userId);
            return Optional.empty();
        }
        List<CashInvestment> cashAccounts = investmentRepository.findByUserId(userId).stream()
                .filter(i -> i instanceof CashInvestment)
                .map(i -> (CashInvestment) i)
                .collect(java.util.stream.Collectors.toList());
        if (cashAccounts.size() == 1) {
            return Optional.of(cashAccounts.get(0));
        }
        if (cashAccounts.isEmpty()) {
            log.info("Expense linkage: user {} has no CASH investment — balance untouched.", userId);
        } else {
            log.info("Expense linkage: user {} has {} cash accounts and no explicit choice — balance untouched.",
                    userId, cashAccounts.size());
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // EXPENSE postings (expense lifecycle + backfill)
    // ------------------------------------------------------------------

    /**
     * Post the EXPENSE debit for an expense. Idempotent: an existing
     * transaction for the same expenseId is removed first (reverse-and-post),
     * so update flows can call this directly after changing the expense.
     *
     * @return the persisted transaction, or {@code null} when no cash account
     *         was resolved (balance untouched, consistent with legacy rows).
     */
    @Transactional
    public Transaction postExpenseTransaction(Long userId, Long expenseId, CashInvestment account,
                                              LocalDate expenseDate, BigDecimal amount, String currency,
                                              String description) {
        deleteByExpenseId(expenseId);

        Transaction txn = Transaction.builder()
                .investmentId(account.getId())
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .transactionDate(expenseDate)
                .currency(currency)
                .expenseId(expenseId)
                .notes("Expense: " + description)
                .build();
        Transaction saved = transactionRepository.save(txn);
        refreshCurrentBalance(account);
        log.info("EXPENSE transaction {} posted for expense {} against cash account {}.",
                saved.getId(), expenseId, account.getId());
        return saved;
    }

    /** Remove the EXPENSE transaction(s) for an expense and refresh the balance. */
    @Transactional
    public void removeExpenseTransaction(Long expenseId) {
        List<Transaction> removed = deleteByExpenseId(expenseId);
        if (removed.isEmpty()) {
            return;
        }
        // Refresh every affected account (normally exactly one).
        removed.stream()
                .map(t -> investmentRepository.findById(t.getInvestmentId()).orElse(null))
                .filter(i -> i instanceof CashInvestment)
                .map(i -> (CashInvestment) i)
                .distinct()
                .forEach(this::refreshCurrentBalance);
        log.info("EXPENSE transaction(s) for expense {} removed; balance(s) refreshed.", expenseId);
    }

    private List<Transaction> deleteByExpenseId(Long expenseId) {
        List<Transaction> existing = transactionRepository.findByExpenseId(expenseId);
        if (!existing.isEmpty()) {
            transactionRepository.deleteAll(existing);
        }
        return existing;
    }
}
