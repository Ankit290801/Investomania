package com.investment.tracker.service;

import com.investment.tracker.dto.PriceData;
import com.investment.tracker.model.Investment;
import com.investment.tracker.model.InvestmentType;
import com.investment.tracker.model.PriceUpdateResult;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.PriceUpdateResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for scheduled market data updates.
 * Automatically fetches prices for all listed equities and cryptos daily.
 */
@Service
public class MarketDataSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataSchedulerService.class);
    
    @Autowired
    private InvestmentRepository investmentRepository;
    
    @Autowired
    private MarketDataAggregatorService marketDataAggregatorService;
    
    @Autowired
    private PriceUpdateResultRepository priceUpdateResultRepository;
    
    @Value("${market-data.scheduler.enabled:true}")
    private boolean schedulerEnabled;
    
    @Value("${market-data.scheduler.batch-size:50}")
    private int batchSize;
    
    @Value("${market-data.scheduler.batch-delay-ms:2000}")
    private long batchDelayMs;
    
    /**
     * Scheduled job to update prices daily at 6 PM IST (18:00)
     * Cron: "0 0 18 * * ?" means: second=0, minute=0, hour=18, every day
     * Note: Time zone handled by server's default time zone
     */
    @Scheduled(cron = "${market-data.scheduler.cron:0 0 18 * * ?}")
    public void scheduledPriceUpdate() {
        if (!schedulerEnabled) {
            logger.info("Scheduled price update is disabled. Skipping.");
            return;
        }
        
        logger.info("=== SCHEDULED PRICE UPDATE STARTED ===");
        
        try {
            PriceUpdateResult result = performPriceUpdate(
                PriceUpdateResult.TriggerType.SCHEDULED,
                "SYSTEM"
            );
            
            logger.info("=== SCHEDULED PRICE UPDATE COMPLETED ===");
            logger.info("Total: {}, Success: {}, Failed: {}, Duration: {}ms, Success Rate: {:.2f}%",
                result.getTotalSymbols(),
                result.getSuccessCount(),
                result.getFailureCount(),
                result.getDurationMs(),
                result.getSuccessRate());
                
        } catch (Exception e) {
            logger.error("Scheduled price update failed with exception: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Manual trigger for price update (callable from REST endpoint)
     */
    @Transactional
    public PriceUpdateResult triggerManualUpdate(String triggeredBy) {
        logger.info("Manual price update triggered by: {}", triggeredBy);
        
        return performPriceUpdate(
            PriceUpdateResult.TriggerType.MANUAL,
            triggeredBy
        );
    }
    
    /**
     * Core method to perform price updates
     */
    @Transactional
    protected PriceUpdateResult performPriceUpdate(
            PriceUpdateResult.TriggerType triggerType,
            String triggeredBy) {
        
        long startTime = System.currentTimeMillis();
        
        // Initialize result tracking
        PriceUpdateResult.PriceUpdateResultBuilder resultBuilder = PriceUpdateResult.builder()
            .updateDate(LocalDateTime.now())
            .triggerType(triggerType)
            .triggeredBy(triggeredBy);
        
        List<String> failedSymbols = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        try {
            // Get all listed investments (EQUITY and CRYPTO with isListed = true)
            List<Investment> listedInvestments = investmentRepository.findAll().stream()
                .filter(inv -> inv.getIsListed() != null && inv.getIsListed())
                .filter(inv -> inv.getType() == InvestmentType.EQUITY || 
                              inv.getType() == InvestmentType.CRYPTO)
                .collect(Collectors.toList());
            
            logger.info("Found {} listed investments to update", listedInvestments.size());
            
            if (listedInvestments.isEmpty()) {
                logger.warn("No listed investments found. Nothing to update.");
                
                long duration = System.currentTimeMillis() - startTime;
                
                return resultBuilder
                    .totalSymbols(0)
                    .successCount(0)
                    .failureCount(0)
                    .durationMs(duration)
                    .status(PriceUpdateResult.UpdateStatus.SUCCESS)
                    .build();
            }
            
            // Group investments into batches
            List<List<Investment>> batches = createBatches(listedInvestments, batchSize);
            
            logger.info("Processing {} batches of {} investments each", batches.size(), batchSize);
            
            // Process each batch
            for (int i = 0; i < batches.size(); i++) {
                List<Investment> batch = batches.get(i);
                
                logger.info("Processing batch {}/{} ({} investments)", 
                    i + 1, batches.size(), batch.size());
                
                for (Investment investment : batch) {
                    try {
                        // Extract symbol and market
                        String symbol = extractSymbol(investment);
                        String market = extractMarket(investment);
                        
                        if (symbol == null || symbol.isEmpty()) {
                            logger.warn("No symbol found for investment ID: {}", investment.getId());
                            failedSymbols.add("ID:" + investment.getId() + " (no symbol)");
                            failureCount++;
                            continue;
                        }
                        
                        // Fetch current price
                        PriceData priceData = marketDataAggregatorService.getPrice(symbol, market);
                        
                        // Update investment value
                        marketDataAggregatorService.updateInvestmentValue(investment.getId());
                        
                        logger.debug("✓ Updated {} = {} {}", 
                            symbol, priceData.getPrice(), priceData.getCurrency());
                        
                        successCount++;
                        
                        // Small delay to avoid hammering APIs
                        Thread.sleep(500);
                        
                    } catch (Exception e) {
                        String symbol = extractSymbol(investment);
                        logger.error("Failed to update investment {}: {}", 
                            symbol != null ? symbol : investment.getId(), 
                            e.getMessage());
                        
                        failedSymbols.add(symbol != null ? symbol : "ID:" + investment.getId());
                        failureCount++;
                    }
                }
                
                // Delay between batches (except after last batch)
                if (i < batches.size() - 1) {
                    logger.info("Batch {} complete. Waiting {}ms before next batch...", 
                        i + 1, batchDelayMs);
                    Thread.sleep(batchDelayMs);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Determine overall status
            PriceUpdateResult.UpdateStatus status;
            if (failureCount == 0) {
                status = PriceUpdateResult.UpdateStatus.SUCCESS;
            } else if (successCount > 0) {
                status = PriceUpdateResult.UpdateStatus.PARTIAL_SUCCESS;
            } else {
                status = PriceUpdateResult.UpdateStatus.FAILURE;
            }
            
            // Build result
            PriceUpdateResult result = resultBuilder
                .totalSymbols(listedInvestments.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .durationMs(duration)
                .failedSymbols(failedSymbols.isEmpty() ? null : String.join(", ", failedSymbols))
                .status(status)
                .build();
            
            // Save result to database
            priceUpdateResultRepository.save(result);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            logger.error("Price update job failed: {}", e.getMessage(), e);
            
            PriceUpdateResult result = resultBuilder
                .totalSymbols(successCount + failureCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .durationMs(duration)
                .failedSymbols(failedSymbols.isEmpty() ? null : String.join(", ", failedSymbols))
                .errorMessage(e.getMessage())
                .status(PriceUpdateResult.UpdateStatus.FAILURE)
                .build();
            
            priceUpdateResultRepository.save(result);
            
            return result;
        }
    }
    
    /**
     * Create batches from list of investments
     */
    private List<List<Investment>> createBatches(List<Investment> investments, int batchSize) {
        List<List<Investment>> batches = new ArrayList<>();
        
        for (int i = 0; i < investments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, investments.size());
            batches.add(investments.subList(i, end));
        }
        
        return batches;
    }
    
    /**
     * Extract symbol from investment using reflection
     */
    private String extractSymbol(Investment investment) {
        try {
            Field symbolField = investment.getClass().getDeclaredField("symbol");
            symbolField.setAccessible(true);
            return (String) symbolField.get(investment);
        } catch (Exception e) {
            logger.debug("No symbol field found for investment type: {}", 
                investment.getType());
            return null;
        }
    }
    
    /**
     * Extract market from investment using reflection
     */
    private String extractMarket(Investment investment) {
        try {
            if (investment.getType() == InvestmentType.EQUITY) {
                Field marketField = investment.getClass().getDeclaredField("market");
                marketField.setAccessible(true);
                return (String) marketField.get(investment);
            } else if (investment.getType() == InvestmentType.CRYPTO) {
                return "CRYPTO";
            }
        } catch (Exception e) {
            logger.debug("Could not extract market for investment: {}", investment.getId());
        }
        
        return "NSE"; // Default fallback
    }
    
    /**
     * Get the most recent update result
     */
    public Optional<PriceUpdateResult> getLastUpdateResult() {
        return priceUpdateResultRepository.findFirstByOrderByUpdateDateDesc();
    }
    
    /**
     * Get update history
     */
    public List<PriceUpdateResult> getUpdateHistory(int limit) {
        List<PriceUpdateResult> all = priceUpdateResultRepository.findAllByOrderByUpdateDateDesc();
        
        if (all.size() <= limit) {
            return all;
        }
        
        return all.subList(0, limit);
    }
}
