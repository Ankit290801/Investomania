package com.investment.tracker.service;

import com.investment.tracker.dto.*;
import com.investment.tracker.model.Investment;
import com.investment.tracker.model.InvestmentType;
import com.investment.tracker.model.PortfolioSnapshot;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating portfolio metrics, net worth, and analytics.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PortfolioCalculationService {

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    // Segment definitions (from business-rules.yml)
    private static final Map<String, List<InvestmentType>> SEGMENT_DEFINITIONS = Map.of(
        "Equity", Arrays.asList(InvestmentType.EQUITY, InvestmentType.PRIVATE_EQUITY),
        "Safe Assets", Arrays.asList(InvestmentType.FD, InvestmentType.RD, InvestmentType.BOND, InvestmentType.PPF, InvestmentType.CASH),
        "Illiquid Assets", Arrays.asList(InvestmentType.REAL_ESTATE, InvestmentType.NPS),
        "Crypto", Collections.singletonList(InvestmentType.CRYPTO)
    );

    /**
     * Calculate total net worth for a user in specified currency.
     * For MVP, we use investment currentValue which is updated by transactions.
     * Future: Add currency conversion support.
     */
    public NetWorthDTO calculateTotalNetWorth(Long userId, String currency) {
        log.info("Calculating net worth for user {} in currency {}", userId, currency);

        List<Investment> investments = investmentRepository.findByUserId(userId);
        
        BigDecimal totalValue = investments.stream()
            .filter(inv -> inv.getCurrentValue() != null)
            .map(inv -> convertCurrency(inv.getCurrentValue(), inv.getCurrency(), currency))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate growth from 1 year ago
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        BigDecimal valueOneYearAgo = calculateNetWorthAtDate(userId, oneYearAgo, currency);
        BigDecimal growth = totalValue.subtract(valueOneYearAgo);
        BigDecimal growthPercentage = valueOneYearAgo.compareTo(BigDecimal.ZERO) > 0 
            ? growth.divide(valueOneYearAgo, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        return NetWorthDTO.builder()
            .totalValue(totalValue)
            .currency(currency)
            .growth(growth)
            .growthPercentage(growthPercentage)
            .build();
    }

    /**
     * Get segment breakdown (Equity, Safe Assets, Illiquid Assets, Crypto).
     */
    public SegmentBreakdownDTO getSegmentBreakdown(Long userId, String currency) {
        log.info("Calculating segment breakdown for user {} in currency {}", userId, currency);

        List<Investment> investments = investmentRepository.findByUserId(userId);
        Map<String, BigDecimal> segments = new HashMap<>();

        // Initialize all segments with zero
        SEGMENT_DEFINITIONS.keySet().forEach(segment -> segments.put(segment, BigDecimal.ZERO));

        // Calculate value for each segment
        for (Investment investment : investments) {
            if (investment.getCurrentValue() == null) continue;
            
            BigDecimal value = convertCurrency(investment.getCurrentValue(), investment.getCurrency(), currency);
            String segment = getSegmentForInvestmentType(investment.getType());
            
            segments.put(segment, segments.get(segment).add(value));
        }

        BigDecimal total = segments.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SegmentBreakdownDTO.builder()
            .segments(segments)
            .total(total)
            .currency(currency)
            .build();
    }

    /**
     * Calculate year-over-year growth and historical net worth.
     * Prefers FY-end PortfolioSnapshots when available; falls back to
     * transaction-replay for dates with no snapshot.
     */
    public GrowthMetricsDTO calculateYoYGrowth(Long userId, int years, String currency) {
        log.info("Calculating YoY growth for user {} for {} years", userId, years);

        List<PortfolioSnapshot> snapshots =
                snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId);

        LocalDate today = LocalDate.now();
        List<HistoricalNetWorthDTO> historicalData = new ArrayList<>();

        if (!snapshots.isEmpty()) {
            // ---- Snapshot-based path (accurate) ----
            // Filter to the requested number of years
            LocalDate cutoff = today.minusYears(years);
            for (PortfolioSnapshot s : snapshots) {
                if (s.getSnapshotDate().isBefore(cutoff)) continue;
                BigDecimal value = convertCurrency(s.getTotalValue(), s.getCurrency(), currency);
                historicalData.add(HistoricalNetWorthDTO.builder()
                        .date(s.getSnapshotDate())
                        .value(value)
                        .currency(currency)
                        .build());
            }
        }

        // Always include the live current value as the final data point
        BigDecimal currentValue = calculateTotalNetWorth(userId, currency).getTotalValue();
        historicalData.add(HistoricalNetWorthDTO.builder()
                .date(today)
                .value(currentValue)
                .currency(currency)
                .build());

        // If no snapshots, fall back to yearly transaction-replay points
        if (snapshots.isEmpty()) {
            for (int i = years - 1; i >= 1; i--) {
                LocalDate date = today.minusYears(i);
                BigDecimal value = calculateNetWorthAtDate(userId, date, currency);
                historicalData.add(0, HistoricalNetWorthDTO.builder()
                        .date(date)
                        .value(value)
                        .currency(currency)
                        .build());
            }
        }

        // Sort ascending
        historicalData.sort(Comparator.comparing(HistoricalNetWorthDTO::getDate));

        // Calculate YoY growth
        BigDecimal oneYearAgoValue = BigDecimal.ZERO;
        LocalDate oneYearAgoDate = today.minusYears(1);
        for (int i = historicalData.size() - 2; i >= 0; i--) {
            if (!historicalData.get(i).getDate().isAfter(oneYearAgoDate)) {
                oneYearAgoValue = historicalData.get(i).getValue();
                break;
            }
        }

        BigDecimal yoyGrowth = currentValue.subtract(oneYearAgoValue);
        BigDecimal yoyGrowthPercentage = oneYearAgoValue.compareTo(BigDecimal.ZERO) > 0
                ? yoyGrowth.divide(oneYearAgoValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal oldestValue = historicalData.get(0).getValue();
        BigDecimal cagr = calculateCAGR(oldestValue, currentValue, years);

        return GrowthMetricsDTO.builder()
                .yoyGrowth(yoyGrowth)
                .yoyGrowthPercentage(yoyGrowthPercentage)
                .cagr(cagr)
                .historicalData(historicalData)
                .currency(currency)
                .build();
    }

    /**
     * Calculate segmental impact on portfolio growth.
     */
    public SegmentImpactDTO getSegmentalImpact(Long userId, String currency) {
        log.info("Calculating segmental impact for user {}", userId);

        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        List<Investment> investments = investmentRepository.findByUserId(userId);

        Map<String, BigDecimal> currentSegmentValues = new HashMap<>();
        Map<String, BigDecimal> pastSegmentValues = new HashMap<>();
        
        // Initialize segments
        SEGMENT_DEFINITIONS.keySet().forEach(segment -> {
            currentSegmentValues.put(segment, BigDecimal.ZERO);
            pastSegmentValues.put(segment, BigDecimal.ZERO);
        });

        // Calculate current and past values for each segment
        for (Investment investment : investments) {
            String segment = getSegmentForInvestmentType(investment.getType());
            
            // Current value
            if (investment.getCurrentValue() != null) {
                BigDecimal currentValue = convertCurrency(investment.getCurrentValue(), investment.getCurrency(), currency);
                currentSegmentValues.put(segment, currentSegmentValues.get(segment).add(currentValue));
            }

            // Past value (1 year ago)
            BigDecimal pastValue = calculateInvestmentValueAtDate(investment, oneYearAgo, currency);
            pastSegmentValues.put(segment, pastSegmentValues.get(segment).add(pastValue));
        }

        // Calculate contributions and returns
        Map<String, BigDecimal> segmentContributions = new HashMap<>();
        Map<String, BigDecimal> segmentReturns = new HashMap<>();
        
        String bestSegment = null;
        String worstSegment = null;
        BigDecimal bestReturn = new BigDecimal("-999999");
        BigDecimal worstReturn = new BigDecimal("999999");

        for (String segment : SEGMENT_DEFINITIONS.keySet()) {
            BigDecimal currentVal = currentSegmentValues.get(segment);
            BigDecimal pastVal = pastSegmentValues.get(segment);
            BigDecimal contribution = currentVal.subtract(pastVal);
            
            BigDecimal returnPercentage = pastVal.compareTo(BigDecimal.ZERO) > 0
                ? contribution.divide(pastVal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

            segmentContributions.put(segment, contribution);
            segmentReturns.put(segment, returnPercentage);

            // Track best and worst
            if (currentVal.compareTo(BigDecimal.ZERO) > 0) {
                if (returnPercentage.compareTo(bestReturn) > 0) {
                    bestReturn = returnPercentage;
                    bestSegment = segment;
                }
                if (returnPercentage.compareTo(worstReturn) < 0) {
                    worstReturn = returnPercentage;
                    worstSegment = segment;
                }
            }
        }

        return SegmentImpactDTO.builder()
            .segmentContributions(segmentContributions)
            .segmentReturns(segmentReturns)
            .bestPerformingSegment(bestSegment)
            .worstPerformingSegment(worstSegment)
            .currency(currency)
            .build();
    }

    /**
     * Calculate net worth at a specific date by replaying transactions.
     */
    private BigDecimal calculateNetWorthAtDate(Long userId, LocalDate date, String currency) {
        List<Investment> investments = investmentRepository.findByUserId(userId);
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Investment investment : investments) {
            BigDecimal value = calculateInvestmentValueAtDate(investment, date, currency);
            totalValue = totalValue.add(value);
        }

        return totalValue;
    }

    /**
     * Calculate investment value at a specific date by replaying transactions up to that date.
     */
    private BigDecimal calculateInvestmentValueAtDate(Investment investment, LocalDate date, String currency) {
        List<Transaction> transactions = transactionRepository
            .findByInvestmentIdAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
                investment.getId(), date);

        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;

        for (Transaction txn : transactions) {
            switch (txn.getType()) {
                case BUY:
                case CONTRIBUTION:
                    if (txn.getQuantity() != null) {
                        totalQuantity = totalQuantity.add(txn.getQuantity());
                    }
                    totalInvested = totalInvested.add(convertCurrency(txn.getAmount(), txn.getCurrency(), currency));
                    break;
                case SELL:
                case WITHDRAWAL:
                    if (txn.getQuantity() != null) {
                        totalQuantity = totalQuantity.subtract(txn.getQuantity());
                    }
                    totalInvested = totalInvested.subtract(convertCurrency(txn.getAmount(), txn.getCurrency(), currency));
                    break;
                case DIVIDEND:
                case INTEREST:
                case BONUS:
                    // These don't affect quantity but add to value
                    totalInvested = totalInvested.add(convertCurrency(txn.getAmount(), txn.getCurrency(), currency));
                    break;
                default:
                    break;
            }
        }

        // For simplicity, use invested amount as value
        // In Phase 7 (Market Data Integration), we'll use market prices
        return totalInvested.max(BigDecimal.ZERO);
    }

    /**
     * Calculate CAGR (Compound Annual Growth Rate).
     */
    private BigDecimal calculateCAGR(BigDecimal startValue, BigDecimal endValue, int years) {
        if (startValue.compareTo(BigDecimal.ZERO) <= 0 || years <= 0) {
            return BigDecimal.ZERO;
        }

        double ratio = endValue.divide(startValue, 10, RoundingMode.HALF_UP).doubleValue();
        double cagr = (Math.pow(ratio, 1.0 / years) - 1.0) * 100.0;
        
        return BigDecimal.valueOf(cagr).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get segment name for an investment type.
     */
    private String getSegmentForInvestmentType(InvestmentType type) {
        for (Map.Entry<String, List<InvestmentType>> entry : SEGMENT_DEFINITIONS.entrySet()) {
            if (entry.getValue().contains(type)) {
                return entry.getKey();
            }
        }
        return "Other";
    }

    /**
     * Convert currency (mock implementation for MVP).
     * In Phase 7, integrate with real forex API.
     */
    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        // Mock conversion rates (1 USD = 83 INR, 1 EUR = 90 INR, 1 GBP = 105 INR)
        Map<String, BigDecimal> toINR = Map.of(
            "USD", BigDecimal.valueOf(83),
            "EUR", BigDecimal.valueOf(90),
            "GBP", BigDecimal.valueOf(105),
            "INR", BigDecimal.ONE
        );

        // Convert to INR first, then to target currency
        BigDecimal inINR = amount.multiply(toINR.getOrDefault(fromCurrency, BigDecimal.ONE));
        BigDecimal result = inINR.divide(toINR.getOrDefault(toCurrency, BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        
        return result;
    }
}
