package com.investment.tracker.service;

import com.investment.tracker.dto.ResolvedSymbol;
import com.investment.tracker.dto.SymbolSearchResult;
import com.investment.tracker.model.InvestmentType;
import com.investment.tracker.model.Market;
import com.investment.tracker.model.SymbolMapping;
import com.investment.tracker.repository.SymbolMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for intelligent symbol resolution and mapping
 * Handles the critical task of converting user symbols to API-specific formats
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SymbolResolutionService {
    
    private final SymbolMappingRepository symbolMappingRepository;
    
    /**
     * Resolve a user symbol to API-specific formats
     * 
     * @param userSymbol User's input symbol (e.g., "RELIANCE", "AAPL")
     * @param market Target market (NSE, BSE, NYSE, etc.) - if null, will try to detect
     * @return ResolvedSymbol with Yahoo and Google formatted symbols
     */
    @Transactional
    public ResolvedSymbol resolveSymbol(String userSymbol, Market market) {
        if (userSymbol == null || userSymbol.trim().isEmpty()) {
            return null;
        }
        
        String normalizedSymbol = userSymbol.trim().toUpperCase();
        log.debug("Resolving symbol: {} for market: {}", normalizedSymbol, market);
        
        // Step 1: Check cache (SymbolMapping table)
        Optional<SymbolMapping> cached = symbolMappingRepository.findByUserSymbol(normalizedSymbol);
        if (cached.isPresent() && isStillValid(cached.get())) {
            log.debug("Found cached mapping for {}", normalizedSymbol);
            return toResolvedSymbol(cached.get(), ResolvedSymbol.ResolutionSource.CACHED);
        }

        // Step 2: Auto-detect market if not provided
        if (market == null) {
            market = detectMarket(normalizedSymbol);
            log.debug("Auto-detected market: {} for symbol: {}", market, normalizedSymbol);
        }

        // Step 3: Apply market-specific transformations
        String yahooSymbol = applyYahooRules(normalizedSymbol, market);
        String googleSymbol = applyGoogleRules(normalizedSymbol, market);

        // Step 4: Save mapping. If a row already exists for this user_symbol
        // (e.g., stale per isStillValid), refresh it in place instead of
        // inserting a duplicate, which would violate the unique constraint.
        SymbolMapping mapping;
        if (cached.isPresent()) {
            mapping = cached.get();
            mapping.setYahooSymbol(yahooSymbol);
            mapping.setGoogleSymbol(googleSymbol);
            mapping.setMarket(market);
            mapping.setAssetType(detectAssetType(normalizedSymbol, market));
            // Keep existing verified flag — markAsVerified is called by callers on success.
            symbolMappingRepository.save(mapping);
            log.info("Refreshed stale symbol mapping: {} -> Yahoo: {}, Google: {}",
                    normalizedSymbol, yahooSymbol, googleSymbol);
        } else {
            mapping = SymbolMapping.builder()
                .userSymbol(normalizedSymbol)
                .yahooSymbol(yahooSymbol)
                .googleSymbol(googleSymbol)
                .market(market)
                .assetType(detectAssetType(normalizedSymbol, market))
                .isVerified(false)
                .build();
            symbolMappingRepository.save(mapping);
            log.info("Created new symbol mapping: {} -> Yahoo: {}, Google: {}",
                    normalizedSymbol, yahooSymbol, googleSymbol);
        }

        return toResolvedSymbol(mapping, ResolvedSymbol.ResolutionSource.AUTO_RESOLVED);
    }
    
    /**
     * Apply Yahoo Finance formatting rules
     */
    private String applyYahooRules(String symbol, Market market) {
        // Remove any existing suffixes/prefixes
        String cleanSymbol = cleanSymbol(symbol);
        
        switch (market) {
            case NSE:
                return cleanSymbol + ".NS";
            case BSE:
                return cleanSymbol + ".BO";
            case NYSE:
            case NASDAQ:
                return cleanSymbol; // No suffix needed for US markets
            case CRYPTO:
                // Ensure crypto symbols end with -USD
                if (!cleanSymbol.endsWith("-USD")) {
                    return cleanSymbol + "-USD";
                }
                return cleanSymbol;
            default:
                return cleanSymbol;
        }
    }
    
    /**
     * Apply Google Finance formatting rules
     */
    private String applyGoogleRules(String symbol, Market market) {
        String cleanSymbol = cleanSymbol(symbol);
        
        switch (market) {
            case NSE:
                return "NSE:" + cleanSymbol;
            case BSE:
                return "BOM:" + cleanSymbol;
            case NYSE:
                return "NYSE:" + cleanSymbol;
            case NASDAQ:
                return "NASDAQ:" + cleanSymbol;
            case CRYPTO:
                // Remove -USD suffix if present for crypto
                String cryptoSymbol = cleanSymbol.replace("-USD", "");
                return "CURRENCY:" + cryptoSymbol + "-USD";
            default:
                return cleanSymbol;
        }
    }
    
    /**
     * Clean symbol by removing common suffixes and prefixes
     */
    private String cleanSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        
        // Remove Yahoo suffixes
        symbol = symbol.replaceAll("\\.(NS|BO|BSE|NSE)$", "");
        
        // Remove Google prefixes
        symbol = symbol.replaceAll("^(NSE|BOM|NYSE|NASDAQ|CURRENCY):", "");
        
        return symbol.trim();
    }
    
    /**
     * Detect market from symbol patterns
     */
    private Market detectMarket(String symbol) {
        // Check for explicit market indicators
        if (symbol.contains(".NS") || symbol.startsWith("NSE:")) {
            return Market.NSE;
        }
        if (symbol.contains(".BO") || symbol.startsWith("BOM:")) {
            return Market.BSE;
        }
        if (symbol.startsWith("NYSE:")) {
            return Market.NYSE;
        }
        if (symbol.startsWith("NASDAQ:")) {
            return Market.NASDAQ;
        }
        if (symbol.endsWith("-USD") || symbol.startsWith("CURRENCY:")) {
            return Market.CRYPTO;
        }
        
        // Check if symbol exists in our database with a known market
        Optional<SymbolMapping> existing = symbolMappingRepository.findByUserSymbol(symbol);
        if (existing.isPresent()) {
            return existing.get().getMarket();
        }
        
        // Default heuristics
        // Common US stock patterns (all caps, 1-5 letters)
        if (symbol.matches("^[A-Z]{1,5}$")) {
            return Market.NASDAQ; // Default to NASDAQ for US stocks
        }
        
        // Default to NSE for longer symbols (likely Indian stocks)
        return Market.NSE;
    }
    
    /**
     * Detect asset type from symbol and market
     */
    private InvestmentType detectAssetType(String symbol, Market market) {
        if (market == Market.CRYPTO) {
            return InvestmentType.CRYPTO;
        }
        // Default to EQUITY for stock symbols
        return InvestmentType.EQUITY;
    }
    
    /**
     * Check if cached mapping is still valid (not too old)
     */
    private boolean isStillValid(SymbolMapping mapping) {
        // If verified and verified recently (within 30 days), consider it valid
        if (mapping.isVerified() && mapping.getLastVerified() != null) {
            return mapping.getLastVerified().isAfter(LocalDateTime.now().minusDays(30));
        }
        // If not verified but created recently (within 7 days), still use it
        if (mapping.getCreatedAt() != null) {
            return mapping.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));
        }
        return true; // Default to valid if no timestamp info
    }
    
    /**
     * Convert SymbolMapping entity to ResolvedSymbol DTO
     */
    private ResolvedSymbol toResolvedSymbol(SymbolMapping mapping, ResolvedSymbol.ResolutionSource source) {
        ResolvedSymbol.ConfidenceLevel confidence;
        
        if (mapping.isVerified()) {
            confidence = ResolvedSymbol.ConfidenceLevel.HIGH;
        } else if (source == ResolvedSymbol.ResolutionSource.CACHED) {
            confidence = ResolvedSymbol.ConfidenceLevel.MEDIUM;
        } else {
            confidence = ResolvedSymbol.ConfidenceLevel.LOW;
        }
        
        return ResolvedSymbol.builder()
            .userSymbol(mapping.getUserSymbol())
            .yahooSymbol(mapping.getYahooSymbol())
            .googleSymbol(mapping.getGoogleSymbol())
            .market(mapping.getMarket())
            .confidence(confidence)
            .verified(mapping.isVerified())
            .name(mapping.getName())
            .source(source)
            .build();
    }
    
    /**
     * Search for symbols by partial match (for autocomplete)
     */
    public List<SymbolSearchResult> searchSymbols(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        
        List<SymbolMapping> results = symbolMappingRepository.searchSymbols(query.trim());
        
        return results.stream()
            .map(this::toSearchResult)
            .collect(Collectors.toList());
    }
    
    /**
     * Convert SymbolMapping to SymbolSearchResult
     */
    private SymbolSearchResult toSearchResult(SymbolMapping mapping) {
        return SymbolSearchResult.builder()
            .symbol(mapping.getUserSymbol())
            .name(mapping.getName())
            .market(mapping.getMarket())
            .assetType(mapping.getAssetType())
            .verified(mapping.isVerified())
            .isin(mapping.getIsin())
            .build();
    }
    
    /**
     * Mark a symbol mapping as verified (called after successful API call)
     */
    @Transactional
    public void markAsVerified(String userSymbol) {
        Optional<SymbolMapping> mapping = symbolMappingRepository.findByUserSymbol(userSymbol);
        if (mapping.isPresent()) {
            mapping.get().markAsVerified();
            symbolMappingRepository.save(mapping.get());
            log.info("Marked symbol mapping as verified: {}", userSymbol);
        }
    }
    
    /**
     * Update or create a symbol mapping (for manual user input)
     */
    @Transactional
    public ResolvedSymbol updateSymbolMapping(String userSymbol, String yahooSymbol, 
                                              String googleSymbol, Market market, String name) {
        Optional<SymbolMapping> existing = symbolMappingRepository.findByUserSymbol(userSymbol);
        
        SymbolMapping mapping;
        if (existing.isPresent()) {
            mapping = existing.get();
            mapping.setYahooSymbol(yahooSymbol);
            mapping.setGoogleSymbol(googleSymbol);
            mapping.setMarket(market);
            mapping.setName(name);
            mapping.setVerified(false); // Reset verification status
        } else {
            mapping = SymbolMapping.builder()
                .userSymbol(userSymbol)
                .yahooSymbol(yahooSymbol)
                .googleSymbol(googleSymbol)
                .market(market)
                .name(name)
                .assetType(detectAssetType(userSymbol, market))
                .isVerified(false)
                .build();
        }
        
        symbolMappingRepository.save(mapping);
        log.info("Updated symbol mapping: {}", userSymbol);
        
        return toResolvedSymbol(mapping, ResolvedSymbol.ResolutionSource.USER_PROVIDED);
    }

    /**
     * Convenience method: resolves a user symbol and returns only the Yahoo Finance
     * formatted symbol string.  Falls back to the raw symbol if resolution fails.
     */
    public String resolveYahooSymbol(String userSymbol, String marketStr) {
        try {
            Market market = marketStr != null ? Market.fromString(marketStr) : null;
            ResolvedSymbol resolved = resolveSymbol(userSymbol, market);
            if (resolved != null && resolved.getYahooSymbol() != null) {
                return resolved.getYahooSymbol();
            }
        } catch (Exception ignored) {
            // Fall through to raw symbol
        }
        return userSymbol;
    }
}
