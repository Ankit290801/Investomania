package com.investment.tracker.service;

import com.investment.tracker.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builds the initial BUY / CONTRIBUTION / DEPOSIT transaction that represents
 * the original acquisition of an investment. Used by InvestmentService when a
 * new investment is created and by the legacy backfill runner.
 */
@Component
@Slf4j
public class InitialTransactionFactory {

    /**
     * Returns the initial transaction for the given investment, or {@code null}
     * if the type does not require one (e.g. unknown / missing required fields).
     *
     * @param investment freshly-persisted investment (must have id and purchaseDate)
     */
    public Transaction buildInitialTransaction(Investment investment) {
        LocalDate txnDate = investment.getPurchaseDate() != null
                ? investment.getPurchaseDate()
                : (investment.getCreatedAt() != null
                    ? investment.getCreatedAt().toLocalDate()
                    : LocalDate.now());

        switch (investment.getType()) {
            case EQUITY:
            case PRIVATE_EQUITY:
                return forEquity((EquityInvestment) investment, txnDate);
            case CRYPTO:
                return forCrypto((CryptoInvestment) investment, txnDate);
            case FD:
                return forFD((FDInvestment) investment, txnDate);
            case RD:
                return forRD((RDInvestment) investment, txnDate);
            case PPF:
                return forPPF((PPFInvestment) investment, txnDate);
            case NPS:
                return forNPS((NPSInvestment) investment, txnDate);
            case BOND:
                return forBond((BondInvestment) investment, txnDate);
            case REAL_ESTATE:
                return forRealEstate((RealEstateInvestment) investment, txnDate);
            case CASH:
                return forCash((CashInvestment) investment, txnDate);
            default:
                log.warn("No initial-transaction template for type {}", investment.getType());
                return null;
        }
    }

    private Transaction forEquity(EquityInvestment e, LocalDate date) {
        if (e.getQuantity() == null || e.getAvgPrice() == null) return null;
        BigDecimal amount = e.getQuantity().multiply(e.getAvgPrice());
        return base(e, TransactionType.BUY, date)
                .quantity(e.getQuantity())
                .pricePerUnit(e.getAvgPrice())
                .amount(amount)
                .notes("Initial purchase (auto-created)")
                .build();
    }

    private Transaction forCrypto(CryptoInvestment c, LocalDate date) {
        if (c.getQuantity() == null || c.getAvgPrice() == null) return null;
        BigDecimal amount = c.getQuantity().multiply(c.getAvgPrice());
        return base(c, TransactionType.BUY, date)
                .quantity(c.getQuantity())
                .pricePerUnit(c.getAvgPrice())
                .amount(amount)
                .notes("Initial purchase (auto-created)")
                .build();
    }

    private Transaction forFD(FDInvestment fd, LocalDate date) {
        if (fd.getPrincipal() == null) return null;
        return base(fd, TransactionType.BUY, date)
                .amount(fd.getPrincipal())
                .notes("FD principal (auto-created)")
                .build();
    }

    private Transaction forRD(RDInvestment rd, LocalDate date) {
        if (rd.getMonthlyContribution() == null) return null;
        return base(rd, TransactionType.CONTRIBUTION, date)
                .amount(rd.getMonthlyContribution())
                .notes("First RD instalment (auto-created)")
                .build();
    }

    private Transaction forPPF(PPFInvestment ppf, LocalDate date) {
        BigDecimal amt = ppf.getTotalContributed();
        if (amt == null || amt.signum() <= 0) return null;
        return base(ppf, TransactionType.CONTRIBUTION, date)
                .amount(amt)
                .notes("PPF total contributed (auto-created)")
                .build();
    }

    private Transaction forNPS(NPSInvestment nps, LocalDate date) {
        BigDecimal amt = nps.getTotalContributed();
        if (amt == null || amt.signum() <= 0) return null;
        return base(nps, TransactionType.CONTRIBUTION, date)
                .amount(amt)
                .notes("NPS total contributed (auto-created)")
                .build();
    }

    private Transaction forBond(BondInvestment b, LocalDate date) {
        BigDecimal amt = b.getCurrentValue() != null ? b.getCurrentValue() : b.getFaceValue();
        if (amt == null) return null;
        return base(b, TransactionType.BUY, date)
                .amount(amt)
                .notes("Bond purchase (auto-created)")
                .build();
    }

    private Transaction forRealEstate(RealEstateInvestment re, LocalDate date) {
        BigDecimal amt = re.getPurchasePrice() != null ? re.getPurchasePrice() : re.getCurrentValue();
        if (amt == null) return null;
        return base(re, TransactionType.BUY, date)
                .amount(amt)
                .notes("Property purchase (auto-created)")
                .build();
    }

    private Transaction forCash(CashInvestment c, LocalDate date) {
        BigDecimal amt = c.getCurrentValue();
        if (amt == null || amt.signum() <= 0) return null;
        return base(c, TransactionType.DEPOSIT, date)
                .amount(amt)
                .notes("Opening balance (auto-created)")
                .build();
    }

    private Transaction.TransactionBuilder base(Investment inv, TransactionType type, LocalDate date) {
        return Transaction.builder()
                .investmentId(inv.getId())
                .type(type)
                .transactionDate(date)
                .currency(inv.getCurrency());
    }
}
