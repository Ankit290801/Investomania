package com.investment.tracker.service;

import com.investment.tracker.model.*;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Calculates the accrued value of interest-bearing instruments
 * (FD, RD, PPF, NPS, Bond, Cash/Savings) at a specific historical date.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestCalculationService {

    private final TransactionRepository transactionRepository;

    // Historical PPF interest rates (FY start April 1 → March 31)
    // Key: year in which March 31 falls  (e.g. 2024 = FY 2023-24)
    private static final java.util.Map<Integer, BigDecimal> PPF_RATES;
    static {
        PPF_RATES = new java.util.HashMap<>();
        PPF_RATES.put(2016, new BigDecimal("8.70"));
        PPF_RATES.put(2017, new BigDecimal("8.10"));
        PPF_RATES.put(2018, new BigDecimal("7.60"));
        PPF_RATES.put(2019, new BigDecimal("8.00"));
        PPF_RATES.put(2020, new BigDecimal("7.90"));
        PPF_RATES.put(2021, new BigDecimal("7.10"));
        PPF_RATES.put(2022, new BigDecimal("7.10"));
        PPF_RATES.put(2023, new BigDecimal("7.10"));
        PPF_RATES.put(2024, new BigDecimal("7.10"));
        PPF_RATES.put(2025, new BigDecimal("7.10"));
        PPF_RATES.put(2026, new BigDecimal("7.10"));
    }

    // ------------------------------------------------------------------
    // Fixed Deposit
    // ------------------------------------------------------------------

    /**
     * Returns the maturity value of an FD as of {@code asOfDate}.
     * Uses compound interest (quarterly compounding by default).
     */
    public BigDecimal calculateFDValue(FDInvestment fd, LocalDate asOfDate) {
        if (fd.getPrincipal() == null || fd.getInterestRate() == null) {
            log.warn("FD {} missing principal or rate – returning currentValue fallback", fd.getId());
            return fd.getCurrentValue() != null ? fd.getCurrentValue() : BigDecimal.ZERO;
        }

        LocalDate startDate = getStartDate(fd);
        if (startDate == null) {
            log.warn("FD {} has no start date – using principal", fd.getId());
            return fd.getPrincipal();
        }

        // Do not project past maturity
        LocalDate effectiveDate = (fd.getMaturityDate() != null && asOfDate.isAfter(fd.getMaturityDate()))
                ? fd.getMaturityDate() : asOfDate;

        if (!effectiveDate.isAfter(startDate)) return fd.getPrincipal();

        long daysElapsed = ChronoUnit.DAYS.between(startDate, effectiveDate);
        BigDecimal rate = fd.getInterestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        // Quarterly compounding: A = P × (1 + r/4)^(4 × t)
        double t = daysElapsed / 365.0;
        double value = fd.getPrincipal().doubleValue() * Math.pow(1 + rate.doubleValue() / 4, 4 * t);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // Recurring Deposit
    // ------------------------------------------------------------------

    /**
     * Calculates the accumulated value of an RD as of {@code asOfDate}.
     * Each monthly instalment is compounded quarterly till maturity.
     */
    public BigDecimal calculateRDValue(RDInvestment rd, LocalDate asOfDate) {
        if (rd.getMonthlyContribution() == null || rd.getInterestRate() == null) {
            return rd.getCurrentValue() != null ? rd.getCurrentValue() : BigDecimal.ZERO;
        }

        LocalDate startDate = getStartDate(rd);
        if (startDate == null) {
            return rd.getCurrentValue() != null ? rd.getCurrentValue() : BigDecimal.ZERO;
        }

        LocalDate effectiveDate = (rd.getMaturityDate() != null && asOfDate.isAfter(rd.getMaturityDate()))
                ? rd.getMaturityDate() : asOfDate;

        if (!effectiveDate.isAfter(startDate)) return BigDecimal.ZERO;

        BigDecimal rate = rd.getInterestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(4), 10, RoundingMode.HALF_UP); // quarterly rate

        long totalMonths = ChronoUnit.MONTHS.between(startDate, effectiveDate);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal p = rd.getMonthlyContribution();

        for (long m = 0; m < totalMonths; m++) {
            double quarters = (totalMonths - m) / 3.0;
            double accumulated = p.doubleValue() * Math.pow(1 + monthlyRate.doubleValue(), quarters);
            total = total.add(BigDecimal.valueOf(accumulated));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // PPF
    // ------------------------------------------------------------------

    /**
     * Estimates PPF balance as of {@code asOfDate} using historical govt rates.
     * Approximation: totalContributed × compound-growth using year-end rates.
     */
    public BigDecimal calculatePPFValue(PPFInvestment ppf, LocalDate asOfDate) {
        if (ppf.getTotalContributed() == null || ppf.getTotalContributed().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        LocalDate startDate = getStartDate(ppf);
        if (startDate == null) return ppf.getTotalContributed();

        // Walk year by year applying the PPF rate for each FY
        BigDecimal balance = BigDecimal.ZERO;
        LocalDate cursor = startDate;

        // Sum contributions before each FY end
        List<com.investment.tracker.model.Transaction> txns =
                transactionRepository.findByInvestmentIdOrderByTransactionDateDesc(ppf.getId());

        // Iterate through financial years from start till asOfDate
        int startFY = cursor.getMonthValue() > 3 ? cursor.getYear() + 1 : cursor.getYear();
        int endFY   = asOfDate.getMonthValue() > 3 ? asOfDate.getYear() + 1 : asOfDate.getYear();

        for (int fy = startFY; fy <= endFY; fy++) {
            LocalDate fyStart = LocalDate.of(fy - 1, 4, 1);
            LocalDate fyEnd   = LocalDate.of(fy, 3, 31);
            LocalDate effectiveFYEnd = fyEnd.isAfter(asOfDate) ? asOfDate : fyEnd;

            // Contributions in this FY
            BigDecimal fyContributions = txns.stream()
                .filter(t -> !t.getTransactionDate().isBefore(fyStart)
                          && !t.getTransactionDate().isAfter(effectiveFYEnd)
                          && (t.getType() == TransactionType.CONTRIBUTION || t.getType() == TransactionType.BUY))
                .map(com.investment.tracker.model.Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            balance = balance.add(fyContributions);

            // Apply interest for this FY (proportional if partial year)
            BigDecimal rate = PPF_RATES.getOrDefault(fy, new BigDecimal("7.10"))
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

            long daysInFY = ChronoUnit.DAYS.between(fyStart, fyEnd.plusDays(1));
            long daysElapsed = ChronoUnit.DAYS.between(fyStart, effectiveFYEnd.plusDays(1));
            double fraction = (double) daysElapsed / daysInFY;

            BigDecimal interest = balance.multiply(rate)
                    .multiply(BigDecimal.valueOf(fraction))
                    .setScale(2, RoundingMode.HALF_UP);
            balance = balance.add(interest);
        }

        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // NPS  (treat similarly to PPF – contribution-based growth)
    // ------------------------------------------------------------------

    /**
     * Returns NPS value as of {@code asOfDate}.
     * For simplicity uses the currentValue if available, otherwise totalContributed
     * with an assumed 10% CAGR (NPS average equity return approximation).
     */
    public BigDecimal calculateNPSValue(NPSInvestment nps, LocalDate asOfDate) {
        if (nps.getCurrentValue() != null) return nps.getCurrentValue();
        if (nps.getTotalContributed() == null) return BigDecimal.ZERO;

        LocalDate startDate = getStartDate(nps);
        if (startDate == null) return nps.getTotalContributed();

        long daysElapsed = ChronoUnit.DAYS.between(startDate, asOfDate);
        if (daysElapsed <= 0) return nps.getTotalContributed();

        double years = daysElapsed / 365.0;
        // Assumed blended NPS return: 10% p.a.
        double value = nps.getTotalContributed().doubleValue() * Math.pow(1.10, years);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // Bond
    // ------------------------------------------------------------------

    /**
     * Returns bond value as face value + accrued coupon interest as of {@code asOfDate}.
     */
    public BigDecimal calculateBondValue(BondInvestment bond, LocalDate asOfDate) {
        if (bond.getFaceValue() == null) {
            return bond.getCurrentValue() != null ? bond.getCurrentValue() : BigDecimal.ZERO;
        }

        LocalDate startDate = getStartDate(bond);
        if (startDate == null) return bond.getFaceValue();

        LocalDate effectiveDate = (bond.getMaturityDate() != null && asOfDate.isAfter(bond.getMaturityDate()))
                ? bond.getMaturityDate() : asOfDate;

        if (!effectiveDate.isAfter(startDate)) return bond.getFaceValue();

        BigDecimal couponRate = bond.getCouponRate() != null ? bond.getCouponRate() : BigDecimal.ZERO;
        long daysElapsed = ChronoUnit.DAYS.between(startDate, effectiveDate);
        BigDecimal accruedInterest = bond.getFaceValue()
                .multiply(couponRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(daysElapsed))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

        return bond.getFaceValue().add(accruedInterest);
    }

    // ------------------------------------------------------------------
    // Cash / Savings Account
    // ------------------------------------------------------------------

    /**
     * Returns savings balance as of {@code asOfDate}.
     * Uses the current value (balance is manually maintained by the user).
     * Optionally applies interest if a rate is configured on the account.
     */
    public BigDecimal calculateCashValue(CashInvestment cash, LocalDate asOfDate) {
        if (cash.getCurrentValue() == null) return BigDecimal.ZERO;

        // If interest rate is configured, apply simple interest approximation
        if (cash.getInterestRate() != null && cash.getInterestRate() > 0) {
            LocalDate startDate = getStartDate(cash);
            if (startDate != null && asOfDate.isAfter(startDate)) {
                long days = ChronoUnit.DAYS.between(startDate, asOfDate);
                BigDecimal interest = cash.getCurrentValue()
                        .multiply(BigDecimal.valueOf(cash.getInterestRate() / 100.0))
                        .multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
                return cash.getCurrentValue().add(interest);
            }
        }
        return cash.getCurrentValue();
    }

    // ------------------------------------------------------------------
    // Real Estate
    // ------------------------------------------------------------------

    /**
     * Returns real estate value.
     * Uses currentValue if set, otherwise purchase price + default 6% annual appreciation.
     */
    public BigDecimal calculateRealEstateValue(RealEstateInvestment re, LocalDate asOfDate) {
        BigDecimal base = re.getCurrentValue() != null ? re.getCurrentValue() : re.getPurchasePrice();
        if (base == null) return BigDecimal.ZERO;

        if (re.getCurrentValue() != null) return re.getCurrentValue();

        // Index from purchase date using 6% p.a.
        LocalDate startDate = getStartDate(re);
        if (startDate == null) return base;

        long days = ChronoUnit.DAYS.between(startDate, asOfDate);
        if (days <= 0) return base;
        double years = days / 365.0;
        double value = base.doubleValue() * Math.pow(1.06, years);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    /**
     * Returns the authoritative acquisition date for an investment:
     * the entity's {@code purchaseDate}, falling back to the earliest transaction
     * date, then to the entity's {@code createdAt}.
     */
    private LocalDate getStartDate(Investment investment) {
        if (investment.getPurchaseDate() != null) return investment.getPurchaseDate();
        LocalDate fromTxn = getEarliestTransactionDate(investment.getId());
        if (fromTxn != null) return fromTxn;
        return investment.getCreatedAt() != null ? investment.getCreatedAt().toLocalDate() : null;
    }

    private LocalDate getEarliestTransactionDate(Long investmentId) {
        List<com.investment.tracker.model.Transaction> txns =
                transactionRepository.findByInvestmentIdOrderByTransactionDateDesc(investmentId);
        if (txns.isEmpty()) return null;
        return txns.stream()
                .map(com.investment.tracker.model.Transaction::getTransactionDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }
}
