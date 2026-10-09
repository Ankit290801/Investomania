package com.investment.tracker.controller;

import com.investment.tracker.dto.*;
import com.investment.tracker.util.SecurityUtil;
import com.investment.tracker.service.PortfolioCalculationService;
import com.investment.tracker.service.RealGrowthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for portfolio analytics and calculations.
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:4173"})
public class PortfolioController {

    private final PortfolioCalculationService portfolioCalculationService;
    private final RealGrowthService realGrowthService;
    private final SecurityUtil securityUtil;

    /**
     * GET /api/portfolio/net-worth?currency=INR
     * Calculate total net worth for the current user.
     */
    @GetMapping("/net-worth")
    public ResponseEntity<NetWorthDTO> getNetWorth(
            @RequestParam(defaultValue = "INR") String currency) {
        
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/portfolio/net-worth - User: {}, Currency: {}", userId, currency);
        
        NetWorthDTO netWorth = portfolioCalculationService.calculateTotalNetWorth(userId, currency);
        return ResponseEntity.ok(netWorth);
    }

    /**
     * GET /api/portfolio/segments?currency=INR
     * Get segment breakdown (Equity, Safe Assets, Illiquid, Crypto).
     */
    @GetMapping("/segments")
    public ResponseEntity<SegmentBreakdownDTO> getSegmentBreakdown(
            @RequestParam(defaultValue = "INR") String currency) {
        
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/portfolio/segments - User: {}, Currency: {}", userId, currency);
        
        SegmentBreakdownDTO breakdown = portfolioCalculationService.getSegmentBreakdown(userId, currency);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * GET /api/portfolio/growth?years=5&currency=INR
     * Get YoY growth and historical net worth data.
     */
    @GetMapping("/growth")
    public ResponseEntity<GrowthMetricsDTO> getGrowthMetrics(
            @RequestParam(defaultValue = "5") int years,
            @RequestParam(defaultValue = "INR") String currency) {
        
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/portfolio/growth - User: {}, Years: {}, Currency: {}", userId, years, currency);
        
        GrowthMetricsDTO growth = portfolioCalculationService.calculateYoYGrowth(userId, years, currency);
        return ResponseEntity.ok(growth);
    }

    /**
     * GET /api/portfolio/real-growth?years=5
     * E13: inflation-adjusted growth in today's rupees (always INR) with a
     * per-fiscal-year breakdown (nominal, real, Y-o-Y inflation, returns)
     * and realized FY XIRR (nominal + CPI-deflated).
     */
    @GetMapping("/real-growth")
    public ResponseEntity<RealGrowthMetricsDTO> getRealGrowth(
            @RequestParam(defaultValue = "5") int years) {

        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/portfolio/real-growth - User: {}, Years: {}", userId, years);

        RealGrowthMetricsDTO realGrowth = realGrowthService.getRealGrowthMetrics(userId, years);
        return ResponseEntity.ok(realGrowth);
    }

    /**
     * GET /api/portfolio/segment-impact?currency=INR
     * Get segmental impact analysis (contributions and returns).
     */
    @GetMapping("/segment-impact")
    public ResponseEntity<SegmentImpactDTO> getSegmentImpact(
            @RequestParam(defaultValue = "INR") String currency) {
        
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/portfolio/segment-impact - User: {}, Currency: {}", userId, currency);
        
        SegmentImpactDTO impact = portfolioCalculationService.getSegmentalImpact(userId, currency);
        return ResponseEntity.ok(impact);
    }
}
