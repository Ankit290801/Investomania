package com.investment.tracker.service;

import com.investment.tracker.model.*;
import com.investment.tracker.repository.AssetValuationRepository;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.PortfolioSnapshotRepository;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the generation and storage of PortfolioSnapshots for each
 * Indian Financial Year end (March 31) since the user's first investment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotGenerationService {

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final AssetValuationRepository assetValuationRepository;
    private final HistoricalPriceService historicalPriceService;
    private final InterestCalculationService interestCalculationService;
    private final SymbolResolutionService symbolResolutionService;

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Generates snapshots for all missing FY-end dates (March 31) since the
     * user's first investment.  Existing snapshots are not recalculated unless
     * {@code force} is true.
     *
     * @return list of saved/updated snapshots
     */
    @Transactional
    public List<PortfolioSnapshot> generateHistoricalSnapshots(Long userId, boolean force) {
        log.info("Generating historical snapshots for user {} (force={})", userId, force);

        List<LocalDate> fyEnds = getFinancialYearEnds(userId);
        if (fyEnds.isEmpty()) {
            log.info("No FY ends found for user {} – no investments yet", userId);
            return Collections.emptyList();
        }

        List<PortfolioSnapshot> results = new ArrayList<>();
        for (LocalDate fyEnd : fyEnds) {
            if (!force && snapshotRepository.existsByUserIdAndSnapshotDate(userId, fyEnd)) {
                log.debug("Snapshot for {} already exists – skipping", fyEnd);
                continue;
            }
            PortfolioSnapshot snapshot = generateSnapshotForDate(userId, fyEnd);
            results.add(snapshot);
        }

        log.info("Generated {} snapshots for user {}", results.size(), userId);
        return results;
    }

    /**
     * (Re)generates the snapshot for a single specific date and persists it.
     */
    @Transactional
    public PortfolioSnapshot generateSnapshotForDate(Long userId, LocalDate date) {
        log.info("Generating snapshot for user {} on {}", userId, date);

        List<Investment> investments = investmentRepository.findByUserId(userId);

        // Only consider investments that existed on or before the snapshot date
        List<Investment> activeInvestments = investments.stream()
                .filter(inv -> wasActiveOnDate(inv, date))
                .collect(Collectors.toList());

        // Breakdown by type
        Map<InvestmentType, BigDecimal> typeValues = new EnumMap<>(InvestmentType.class);
        int estimatedCount = 0;

        for (Investment inv : activeInvestments) {
            AssetValuationResult result = calculateValue(inv, date);

            // Save per-asset valuation
            saveAssetValuation(inv, date, result);

            typeValues.merge(inv.getType(), result.value, BigDecimal::add);
            if (result.source == ValuationSource.ESTIMATED) estimatedCount++;
        }

        BigDecimal total = typeValues.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        SnapshotStatus status = estimatedCount == 0 ? SnapshotStatus.CALCULATED
                : (estimatedCount == activeInvestments.size() ? SnapshotStatus.PARTIAL
                : SnapshotStatus.PARTIAL);

        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .userId(userId)
                .snapshotDate(date)
                .totalValue(total)
                .currency("INR")
                .equityValue(typeValues.getOrDefault(InvestmentType.EQUITY, BigDecimal.ZERO))
                .privateEquityValue(typeValues.getOrDefault(InvestmentType.PRIVATE_EQUITY, BigDecimal.ZERO))
                .fixedDepositValue(typeValues.getOrDefault(InvestmentType.FD, BigDecimal.ZERO))
                .rdValue(typeValues.getOrDefault(InvestmentType.RD, BigDecimal.ZERO))
                .ppfValue(typeValues.getOrDefault(InvestmentType.PPF, BigDecimal.ZERO))
                .npsValue(typeValues.getOrDefault(InvestmentType.NPS, BigDecimal.ZERO))
                .bondValue(typeValues.getOrDefault(InvestmentType.BOND, BigDecimal.ZERO))
                .savingsValue(typeValues.getOrDefault(InvestmentType.CASH, BigDecimal.ZERO))
                .cryptoValue(typeValues.getOrDefault(InvestmentType.CRYPTO, BigDecimal.ZERO))
                .realEstateValue(typeValues.getOrDefault(InvestmentType.REAL_ESTATE, BigDecimal.ZERO))
                .status(status)
                .estimatedCount(estimatedCount)
                .build();

        // Upsert: delete old snapshot for same date if exists
        snapshotRepository.findByUserIdAndSnapshotDate(userId, date)
                .ifPresent(old -> snapshotRepository.delete(old));

        return snapshotRepository.save(snapshot);
    }

    /**
     * Returns all snapshots for a user (newest first).
     */
    public List<PortfolioSnapshot> getSnapshots(Long userId) {
        return snapshotRepository.findByUserIdOrderBySnapshotDateDesc(userId);
    }

    /**
     * Deletes the snapshot for a specific user + date.
     */
    @Transactional
    public void deleteSnapshot(Long userId, LocalDate date) {
        snapshotRepository.deleteByUserIdAndSnapshotDate(userId, date);
        log.info("Deleted snapshot for user {} on {}", userId, date);
    }

    /**
     * Returns a report of which investments lack historical prices on any FY-end
     * for this user.
     */
    public MissingDataReport getMissingDataReport(Long userId) {
        List<PortfolioSnapshot> snapshots = snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId);
        int totalPartial = (int) snapshots.stream()
                .filter(s -> s.getStatus() == SnapshotStatus.PARTIAL)
                .count();
        int estimatedAssets = snapshots.stream()
                .mapToInt(s -> s.getEstimatedCount() != null ? s.getEstimatedCount() : 0)
                .sum();

        return new MissingDataReport(snapshots.size(), totalPartial, estimatedAssets);
    }

    // ------------------------------------------------------------------
    // Value calculation dispatcher
    // ------------------------------------------------------------------

    private AssetValuationResult calculateValue(Investment investment, LocalDate date) {
        try {
            switch (investment.getType()) {
                case EQUITY:
                case PRIVATE_EQUITY:
                    return calculateEquityValue((EquityInvestment) investment, date);

                case CRYPTO:
                    return calculateCryptoValue((CryptoInvestment) investment, date);

                case FD:
                    BigDecimal fdVal = interestCalculationService.calculateFDValue(
                            (FDInvestment) investment, date);
                    return new AssetValuationResult(fdVal, null, null,
                            ValuationSource.CALCULATED, "FD compound interest formula");

                case RD:
                    BigDecimal rdVal = interestCalculationService.calculateRDValue(
                            (RDInvestment) investment, date);
                    return new AssetValuationResult(rdVal, null, null,
                            ValuationSource.CALCULATED, "RD quarterly compounding formula");

                case PPF:
                    BigDecimal ppfVal = interestCalculationService.calculatePPFValue(
                            (PPFInvestment) investment, date);
                    return new AssetValuationResult(ppfVal, null, null,
                            ValuationSource.CALCULATED, "PPF historical rate schedule");

                case NPS:
                    BigDecimal npsVal = interestCalculationService.calculateNPSValue(
                            (NPSInvestment) investment, date);
                    return new AssetValuationResult(npsVal, null, null,
                            ValuationSource.CALCULATED, "NPS estimated 10% CAGR");

                case BOND:
                    BigDecimal bondVal = interestCalculationService.calculateBondValue(
                            (BondInvestment) investment, date);
                    return new AssetValuationResult(bondVal, null, null,
                            ValuationSource.CALCULATED, "Bond face value + accrued coupon");

                case CASH:
                    BigDecimal cashVal = interestCalculationService.calculateCashValue(
                            (CashInvestment) investment, date);
                    return new AssetValuationResult(cashVal, null, null,
                            ValuationSource.CALCULATED, "Cash/Savings balance");

                case REAL_ESTATE:
                    BigDecimal reVal = interestCalculationService.calculateRealEstateValue(
                            (RealEstateInvestment) investment, date);
                    return new AssetValuationResult(reVal, null, null,
                            ValuationSource.ESTIMATED, "Real estate 6% annual appreciation");

                default:
                    log.warn("Unknown investment type {} for id {} – using currentValue",
                            investment.getType(), investment.getId());
                    BigDecimal fallback = investment.getCurrentValue() != null
                            ? investment.getCurrentValue() : BigDecimal.ZERO;
                    return new AssetValuationResult(fallback, null, null,
                            ValuationSource.ESTIMATED, "Unknown type – currentValue fallback");
            }
        } catch (Exception e) {
            log.error("Error calculating value for investment {} on {}: {}",
                    investment.getId(), date, e.getMessage());
            BigDecimal fallback = investment.getCurrentValue() != null
                    ? investment.getCurrentValue() : BigDecimal.ZERO;
            return new AssetValuationResult(fallback, null, null,
                    ValuationSource.ESTIMATED, "Error – currentValue fallback: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Market-price based (Equity / Crypto)
    // ------------------------------------------------------------------

    private AssetValuationResult calculateEquityValue(EquityInvestment equity, LocalDate date) {
        BigDecimal quantity = getQuantityAtDate(equity.getId(), date);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            // No transactions yet → assume the recorded quantity has been held since purchaseDate
            quantity = equity.getQuantity() != null ? equity.getQuantity() : BigDecimal.ZERO;
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return new AssetValuationResult(BigDecimal.ZERO, BigDecimal.ZERO, null,
                    ValuationSource.CALCULATED, "Zero quantity on date");
        }

        // Resolve Yahoo symbol
        String yahooSymbol = resolveYahooSymbol(equity.getSymbol(), equity.getMarket());
        BigDecimal price = historicalPriceService.getHistoricalPrice(yahooSymbol, date);

        if (price == null) {
            log.warn("No historical price for {} on {} – using avgPrice as fallback",
                    yahooSymbol, date);
            price = equity.getAvgPrice();
            if (price == null) {
                return new AssetValuationResult(BigDecimal.ZERO, quantity, null,
                        ValuationSource.ESTIMATED, "No price data – zero fallback");
            }
            return new AssetValuationResult(
                    quantity.multiply(price).setScale(2, RoundingMode.HALF_UP),
                    quantity, price, ValuationSource.ESTIMATED, "avgPrice fallback");
        }

        return new AssetValuationResult(
                quantity.multiply(price).setScale(2, RoundingMode.HALF_UP),
                quantity, price, ValuationSource.MARKET_DATA, "Yahoo Finance historical");
    }

    private AssetValuationResult calculateCryptoValue(CryptoInvestment crypto, LocalDate date) {
        BigDecimal quantity = getQuantityAtDate(crypto.getId(), date);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            quantity = crypto.getQuantity() != null ? crypto.getQuantity() : BigDecimal.ZERO;
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return new AssetValuationResult(BigDecimal.ZERO, BigDecimal.ZERO, null,
                    ValuationSource.CALCULATED, "Zero quantity on date");
        }

        // Crypto symbols in Yahoo format: BTC-USD, ETH-USD …
        String symbol = crypto.getSymbol().toUpperCase();
        if (!symbol.contains("-")) symbol = symbol + "-USD";

        BigDecimal price = historicalPriceService.getHistoricalPrice(symbol, date);

        if (price == null) {
            log.warn("No historical price for crypto {} on {} – using avgPrice fallback",
                    symbol, date);
            price = crypto.getAvgPrice();
            if (price == null) {
                return new AssetValuationResult(BigDecimal.ZERO, quantity, null,
                        ValuationSource.ESTIMATED, "No crypto price – zero fallback");
            }
            return new AssetValuationResult(
                    quantity.multiply(price).setScale(2, RoundingMode.HALF_UP),
                    quantity, price, ValuationSource.ESTIMATED, "avgPrice fallback");
        }

        return new AssetValuationResult(
                quantity.multiply(price).setScale(2, RoundingMode.HALF_UP),
                quantity, price, ValuationSource.MARKET_DATA, "Yahoo Finance crypto historical");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Computes net quantity held for an investment on a given date by replaying
     * BUY/SELL transactions up to (and including) that date.
     */
    private BigDecimal getQuantityAtDate(Long investmentId, LocalDate date) {
        List<Transaction> txns = transactionRepository
                .findByInvestmentIdAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
                        investmentId, date);

        BigDecimal qty = BigDecimal.ZERO;
        for (Transaction t : txns) {
            if (t.getQuantity() == null) continue;
            switch (t.getType()) {
                case BUY:
                case CONTRIBUTION:
                case BONUS:
                    qty = qty.add(t.getQuantity());
                    break;
                case SELL:
                case WITHDRAWAL:
                    qty = qty.subtract(t.getQuantity());
                    break;
                default:
                    break;
            }
        }
        return qty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : qty;
    }

    /**
     * Returns true if the investment was active (acquired and not fully exited)
     * on the given snapshot date. Uses purchaseDate as the authoritative source,
     * falling back to createdAt and finally to transaction history.
     */
    private boolean wasActiveOnDate(Investment investment, LocalDate date) {
        LocalDate effectiveStart = investment.getPurchaseDate();
        if (effectiveStart == null && investment.getCreatedAt() != null) {
            effectiveStart = investment.getCreatedAt().toLocalDate();
        }
        if (effectiveStart != null) {
            return !effectiveStart.isAfter(date);
        }
        // Last resort: any transaction on/before date
        List<Transaction> txns = transactionRepository
                .findByInvestmentIdAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
                        investment.getId(), date);
        return !txns.isEmpty();
    }

    /**
     * Persist/update the per-asset valuation record.
     */
    private void saveAssetValuation(Investment investment, LocalDate date,
                                    AssetValuationResult result) {
        // Remove old record for same investment + date
        assetValuationRepository.findByInvestmentIdAndValuationDate(investment.getId(), date)
                .ifPresent(assetValuationRepository::delete);

        AssetValuation av = AssetValuation.builder()
                .investmentId(investment.getId())
                .valuationDate(date)
                .value(result.value)
                .quantity(result.quantity)
                .pricePerUnit(result.pricePerUnit)
                .source(result.source)
                .sourceDetails(result.sourceDetails)
                .build();
        assetValuationRepository.save(av);
    }

    /**
     * Build a list of Indian financial-year end dates (March 31) covering the
     * earliest investment activity up to today.
     *
     * Source of truth: the earliest {@code purchaseDate} on the user's investments.
     * Falls back to the earliest transaction date, then to investment {@code createdAt}.
     */
    private List<LocalDate> getFinancialYearEnds(Long userId) {
        List<Investment> investments = investmentRepository.findByUserId(userId);

        LocalDate earliest = investments.stream()
                .map(Investment::getPurchaseDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        if (earliest == null) {
            List<Transaction> allTxns = transactionRepository.findByUserId(userId);
            earliest = allTxns.stream()
                    .map(Transaction::getTransactionDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);
        }

        if (earliest == null) {
            earliest = investments.stream()
                    .map(Investment::getCreatedAt)
                    .filter(Objects::nonNull)
                    .map(java.time.LocalDateTime::toLocalDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);
        }

        if (earliest == null) return Collections.emptyList();

        LocalDate today = LocalDate.now();
        List<LocalDate> fyEnds = new ArrayList<>();

        // First FY end >= earliest acquisition date
        int startYear = earliest.getMonthValue() > 3 ? earliest.getYear() + 1 : earliest.getYear();

        for (int year = startYear; year <= today.getYear() + 1; year++) {
            LocalDate fyEnd = LocalDate.of(year, 3, 31);
            if (!fyEnd.isAfter(today)) {
                fyEnds.add(fyEnd);
            }
        }
        return fyEnds;
    }

    private String resolveYahooSymbol(String symbol, String market) {
        try {
            return symbolResolutionService.resolveYahooSymbol(symbol, market);
        } catch (Exception e) {
            return symbol; // Fallback to raw symbol
        }
    }

    // ------------------------------------------------------------------
    // Inner types
    // ------------------------------------------------------------------

    /** Lightweight value-object returned by each calculation path. */
    static class AssetValuationResult {
        final BigDecimal value;
        final BigDecimal quantity;
        final BigDecimal pricePerUnit;
        final ValuationSource source;
        final String sourceDetails;

        AssetValuationResult(BigDecimal value, BigDecimal quantity, BigDecimal pricePerUnit,
                              ValuationSource source, String sourceDetails) {
            this.value = value != null ? value : BigDecimal.ZERO;
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
            this.source = source;
            this.sourceDetails = sourceDetails;
        }
    }

    /** Summary DTO for the missing-data report endpoint. */
    public static class MissingDataReport {
        public final int totalSnapshots;
        public final int partialSnapshots;
        public final int estimatedAssets;

        MissingDataReport(int totalSnapshots, int partialSnapshots, int estimatedAssets) {
            this.totalSnapshots = totalSnapshots;
            this.partialSnapshots = partialSnapshots;
            this.estimatedAssets = estimatedAssets;
        }
    }
}
