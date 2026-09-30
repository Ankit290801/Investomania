package com.investment.tracker.config;

import com.investment.tracker.model.CashInvestment;
import com.investment.tracker.model.Expense;
import com.investment.tracker.repository.ExpenseRepository;
import com.investment.tracker.repository.TransactionRepository;
import com.investment.tracker.service.CashLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * E10 one-off (idempotent) migration: give every pre-existing expense its
 * EXPENSE transaction so spending flows through the cash ledger.
 *
 * <p>Replay-based balances make this safe without markers or compensation
 * arithmetic: a cash account's balance is recomputed from its opening
 * DEPOSIT seed and full flow history, so the old direct {@code currentValue}
 * mutations (the legacy Excel-import path) are simply superseded. Excel-
 * imported expenses therefore deduct exactly once (now via the ledger), and
 * manual expenses — which never moved the balance before — start doing so.
 * Rerunning never duplicates: expenses already linked (post-E10 rows, or
 * rows this runner processed before) are skipped.</p>
 *
 * <p>Explicitly chosen accounts win; otherwise linkage happens only when the
 * user has exactly one cash account (the same rule as the live expense
 * path). Cross-currency expenses are skipped with a warning, matching the
 * legacy import behaviour of not mixing currencies into one account.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(30) // after PortfolioBackfillRunner (20): opening DEPOSIT seeds exist first
public class ExpenseCashLedgerBackfillRunner implements CommandLineRunner {

    private final ExpenseRepository expenseRepository;
    private final TransactionRepository transactionRepository;
    private final CashLedgerService cashLedgerService;

    @Override
    @Transactional
    public void run(String... args) {
        List<Expense> expenses = expenseRepository.findAll();
        if (expenses.isEmpty()) {
            return; // fresh database — nothing to migrate
        }

        int posted = 0;
        int alreadyLinked = 0;
        int noSingleAccount = 0;
        int crossCurrency = 0;

        for (Expense expense : expenses) {
            if (expense.getPaidFromInvestmentId() != null
                    || !transactionRepository.findByExpenseId(expense.getId()).isEmpty()) {
                alreadyLinked++; // post-E10 row, or an earlier run got interrupted mid-way
                continue;
            }

            Optional<CashInvestment> account =
                    cashLedgerService.resolveCashAccount(expense.getUserId(), null);
            if (account.isEmpty()) {
                noSingleAccount++;
                continue;
            }
            CashInvestment cash = account.get();

            if (expense.getCurrency() != null && !expense.getCurrency().equals(cash.getCurrency())) {
                log.warn("Backfill: expense {} ({}) skipped — cash account {} is denominated in {}.",
                        expense.getId(), expense.getCurrency(), cash.getId(), cash.getCurrency());
                crossCurrency++;
                continue;
            }

            cashLedgerService.postExpenseTransaction(expense.getUserId(), expense.getId(),
                    cash, expense.getExpenseDate(), expense.getAmount(),
                    expense.getCurrency(), expense.getDescription());
            expense.setPaidFromInvestmentId(cash.getId());
            expenseRepository.save(expense);
            posted++;
        }

        if (posted > 0 || noSingleAccount > 0 || crossCurrency > 0) {
            log.info("Expense-to-cash backfill: {} EXPENSE transaction(s) posted, {} already linked, "
                            + "{} skipped (no single cash account), {} skipped (cross-currency), "
                            + "out of {} expense(s).",
                    posted, alreadyLinked, noSingleAccount, crossCurrency, expenses.size());
        }
    }
}
