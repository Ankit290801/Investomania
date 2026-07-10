package com.investment.tracker.controller;

import com.investment.tracker.dto.ResolvedSymbol;
import com.investment.tracker.dto.SymbolMappingRequest;
import com.investment.tracker.dto.SymbolSearchResult;
import com.investment.tracker.model.Market;
import com.investment.tracker.service.SymbolResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * REST controller for symbol resolution and mapping operations
 */
@RestController
@RequestMapping("/api/symbols")
@RequiredArgsConstructor
@Slf4j
public class SymbolController {
    
    private final SymbolResolutionService symbolResolutionService;
    
    /**
     * Resolve a user symbol to API-specific formats
     * 
     * GET /api/symbols/resolve?symbol=RELIANCE&market=NSE
     */
    @GetMapping("/resolve")
    public ResponseEntity<ResolvedSymbol> resolveSymbol(
            @RequestParam String symbol,
            @RequestParam(required = false) Market market) {
        
        log.info("Resolving symbol: {} for market: {}", symbol, market);
        
        ResolvedSymbol resolved = symbolResolutionService.resolveSymbol(symbol, market);
        
        if (resolved == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(resolved);
    }
    
    /**
     * Search for symbols by partial match (autocomplete)
     * 
     * GET /api/symbols/search?q=REL
     */
    @GetMapping("/search")
    public ResponseEntity<List<SymbolSearchResult>> searchSymbols(@RequestParam("q") String query) {
        log.info("Searching symbols with query: {}", query);
        
        List<SymbolSearchResult> results = symbolResolutionService.searchSymbols(query);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * Update or create a symbol mapping (manual user input)
     * 
     * POST /api/symbols/mapping
     * Body: { "userSymbol": "RELIANCE", "yahooSymbol": "RELIANCE.NS", "googleSymbol": "NSE:RELIANCE", "market": "NSE", "name": "Reliance Industries" }
     */
    @PostMapping("/mapping")
    public ResponseEntity<ResolvedSymbol> updateSymbolMapping(@Valid @RequestBody SymbolMappingRequest request) {
        log.info("Updating symbol mapping: {}", request.getUserSymbol());
        
        ResolvedSymbol updated = symbolResolutionService.updateSymbolMapping(
            request.getUserSymbol(),
            request.getYahooSymbol(),
            request.getGoogleSymbol(),
            request.getMarket(),
            request.getName()
        );
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Mark a symbol as verified (called internally after successful API call)
     * 
     * POST /api/symbols/{symbol}/verify
     */
    @PostMapping("/{symbol}/verify")
    public ResponseEntity<Void> markAsVerified(@PathVariable String symbol) {
        log.info("Marking symbol as verified: {}", symbol);
        
        symbolResolutionService.markAsVerified(symbol);
        
        return ResponseEntity.ok().build();
    }
}
