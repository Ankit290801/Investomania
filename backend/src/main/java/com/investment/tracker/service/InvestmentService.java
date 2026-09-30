package com.investment.tracker.service;

import com.investment.tracker.config.CouponRules;
import com.investment.tracker.config.InvestmentListingRules;
import com.investment.tracker.dto.InvestmentDTO;
import com.investment.tracker.exception.InvestmentNotFoundException;
import com.investment.tracker.exception.UnauthorizedAccessException;
import com.investment.tracker.model.BondInvestment;
import com.investment.tracker.model.Investment;
import com.investment.tracker.model.InvestmentType;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import com.investment.tracker.util.InvestmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for investment management business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final InvestmentMapper investmentMapper;
    private final InitialTransactionFactory initialTransactionFactory;
    private final MarketDataAggregatorService marketDataAggregatorService;
    private final InvestmentListingRules investmentListingRules;
    private final CouponRules couponRules;
    private final BondCouponService bondCouponService;

    /**
     * Create a new investment for a user.
     * Also creates the initial BUY/CONTRIBUTION/DEPOSIT transaction so the
     * historical valuation engine sees the asset as active from purchaseDate.
     */
    public InvestmentDTO createInvestment(Long userId, InvestmentDTO investmentDTO) {
        Investment investment = investmentMapper.toEntity(investmentDTO, userId);
        if (investment.getPurchaseDate() == null) {
            investment.setPurchaseDate(LocalDate.now());
        }
        applyListingRule(investment);
        // For listed EQUITY/CRYPTO: currentValue is owned by the live-price refresh.
        // Leave it null on insert so we never persist qty × avgPrice into current_value.
        // For other types (FD/PPF/etc.) the form supplies the correct currentValue.
        if (isListedSecurity(investment)) {
            investment.setCurrentValue(null);
        }
        applyCouponDefaults(investment);
        Investment savedInvestment = investmentRepository.save(investment);

        Transaction initial = initialTransactionFactory.buildInitialTransaction(savedInvestment);
        if (initial != null) {
            transactionRepository.save(initial);
            log.info("Auto-created {} transaction for investment {} on {}",
                    initial.getType(), savedInvestment.getId(), initial.getTransactionDate());
        }

        // Record any coupon already due between firstCouponDate and today
        // (e.g. a bond bought mid-period).
        if (savedInvestment instanceof BondInvestment) {
            bondCouponService.recordDueCouponsForBond((BondInvestment) savedInvestment, LocalDate.now());
        }

        if(isListedSecurity(savedInvestment)) {
            refreshLivePrice(savedInvestment);
        }

        // Re-fetch so we return the freshly priced row.
        Investment refreshed = investmentRepository.findById(savedInvestment.getId())
                .orElse(savedInvestment);
        InvestmentDTO dto = investmentMapper.toDTO(refreshed);
        dto.setPurchaseDate(refreshed.getPurchaseDate());
        return dto;
    }

    /**
     * Update an existing investment.
     * Preserves the original purchaseDate so historical snapshots remain stable.
     */
    public InvestmentDTO updateInvestment(Long userId, Long investmentId, InvestmentDTO investmentDTO) {
        Investment existingInvestment = investmentRepository.findByIdAndUserId(investmentId, userId)
                .orElseThrow(() -> new InvestmentNotFoundException(investmentId));

        // Convert DTO to entity and preserve immutable fields
        Investment updatedInvestment = investmentMapper.toEntity(investmentDTO, userId);
        updatedInvestment.setId(existingInvestment.getId());
        updatedInvestment.setCreatedAt(existingInvestment.getCreatedAt());
        applyListingRule(updatedInvestment);
        // Never overwrite purchaseDate from the update payload — it's set at creation.
        updatedInvestment.setPurchaseDate(existingInvestment.getPurchaseDate());
        if (isListedSecurity(updatedInvestment)) {
            // Preserve the existing live-price-fed currentValue; refresh happens below.
            updatedInvestment.setCurrentValue(existingInvestment.getCurrentValue());
        }

        Investment savedInvestment = investmentRepository.save(updatedInvestment);
        if (savedInvestment instanceof BondInvestment) {
            bondCouponService.recordDueCouponsForBond((BondInvestment) savedInvestment, LocalDate.now());        }
        refreshLivePrice(savedInvestment);
        Investment refreshed = investmentRepository.findById(savedInvestment.getId())
                .orElse(savedInvestment);
        return investmentMapper.toDTO(refreshed);
    }

    /**
     * Delete an investment and all associated transactions.
     */
    public void deleteInvestment(Long userId, Long investmentId) {
        if (!investmentRepository.existsByIdAndUserId(investmentId, userId)) {
            throw new InvestmentNotFoundException(investmentId);
        }

        // Delete all transactions first
        transactionRepository.deleteByInvestmentId(investmentId);
        
        // Then delete the investment
        investmentRepository.deleteById(investmentId);
    }

    /**
     * Get a single investment by ID.
     */
    @Transactional(readOnly = true)
    public InvestmentDTO getInvestment(Long userId, Long investmentId) {
        Investment investment = investmentRepository.findByIdAndUserId(investmentId, userId)
                .orElseThrow(() -> new InvestmentNotFoundException(investmentId));
        InvestmentDTO dto = investmentMapper.toDTO(investment);
        if (dto.getPurchaseDate() == null) {
            Map<Long, LocalDate> buyDates = transactionRepository.getEarliestBuyDateMap(List.of(investmentId));
            dto.setPurchaseDate(buyDates.get(investmentId));
        }
        return dto;
    }

    /**
     * Get all investments for a user.
     */
    @Transactional(readOnly = true)
    public List<InvestmentDTO> getAllInvestments(Long userId) {
        List<Investment> investments = investmentRepository.findByUserId(userId);
        List<InvestmentDTO> dtos = investments.stream()
                .map(investmentMapper::toDTO)
                .collect(Collectors.toList());
        // Fallback for any DTO whose entity has no purchaseDate (legacy data)
        List<Long> missingIds = dtos.stream()
                .filter(d -> d.getPurchaseDate() == null)
                .map(InvestmentDTO::getId)
                .collect(Collectors.toList());
        if (!missingIds.isEmpty()) {
            Map<Long, LocalDate> buyDates = transactionRepository.getEarliestBuyDateMap(missingIds);
            dtos.forEach(dto -> {
                if (dto.getPurchaseDate() == null) {
                    dto.setPurchaseDate(buyDates.get(dto.getId()));
                }
            });
        }
        return dtos;
    }

    /**
     * Get investments filtered by type.
     */
    @Transactional(readOnly = true)
    public List<InvestmentDTO> getInvestmentsByType(Long userId, InvestmentType type) {
        List<Investment> investments = investmentRepository.findByUserIdAndType(userId, type);
        return investments.stream()
                .map(investmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get investment count for a user.
     */
    @Transactional(readOnly = true)
    public long getInvestmentCount(Long userId) {
        return investmentRepository.countByUserId(userId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Fills in coupon-schedule defaults for bonds that specify a frequency
     * but no first coupon date: derives firstCouponDate from config. Bonds
     * with no frequency keep no schedule (no auto coupons).
     */
    private void applyCouponDefaults(Investment investment) {
        if (!(investment instanceof BondInvestment)) {
            return;
        }
        BondInvestment bond = (BondInvestment) investment;
        if (bond.getAutoGenerateIncome() == null) {
            bond.setAutoGenerateIncome(Boolean.TRUE);
        }
        if (bond.getCouponFrequency() != null && bond.getFirstCouponDate() == null) {
            LocalDate anchor = bond.getPurchaseDate() != null
                    ? bond.getPurchaseDate()
                    : (investment.getCreatedAt() != null
                        ? investment.getCreatedAt().toLocalDate()
                        : LocalDate.now());
            bond.setFirstCouponDate(couponRules.deriveFirstCouponDate(anchor));
        }
    }

    /**
     * INV-3: only auto-price-eligible types (config-driven, currently EQUITY/CRYPTO)
     * may carry {@code is_listed = true}. Every other type is coerced to false so a
     * misbehaving client can never poison the DB. Null on an eligible type falls back
     * to the configured default.
     */
    private void applyListingRule(Investment investment) {
        String typeName = investment.getType() != null ? investment.getType().name() : null;
        if (!investmentListingRules.isAutoPriceEligible(typeName)) {
            investment.setIsListed(false);
            return;
        }
        if (investment.getIsListed() == null) {
            investment.setIsListed(investmentListingRules.isListedByDefault(typeName));
        }
    }

    /** Listed securities have their current value owned by the live-price feed. */
    private boolean isListedSecurity(Investment investment) {
        return Boolean.TRUE.equals(investment.getIsListed())
                && investmentListingRules.isAutoPriceEligible(
                        investment.getType() != null ? investment.getType().name() : null);
    }

    /**
     * For listed equity/crypto, replace currentValue with quantity × live market price
     * via the market-data aggregator (Yahoo → Google → cache, with FX conversion).
     * Leaves currentValue untouched (null on create) if no price source responds.
     */
    private void refreshLivePrice(Investment investment) {
        if (!isListedSecurity(investment)) {
            return;
        }
        try {
            marketDataAggregatorService.updateInvestmentValue(investment.getId());
        } catch (Exception e) {
            log.warn("Live price refresh failed for investment {} ({}); current_value left as-is. Reason: {}",
                    investment.getId(), investment.getType(), e.getMessage());
        }
    }
}
