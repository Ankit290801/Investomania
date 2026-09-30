package com.investment.tracker.service;

import com.investment.tracker.config.InvestmentListingRules;
import com.investment.tracker.dto.FDInvestmentDTO;
import com.investment.tracker.dto.InvestmentDTO;
import com.investment.tracker.model.CashInvestment;
import com.investment.tracker.model.FDInvestment;
import com.investment.tracker.model.Investment;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import com.investment.tracker.util.InvestmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for bug INV-3: the server must force is_listed=false for
 * types that cannot be auto-priced, regardless of what the client sends.
 */
@ExtendWith(MockitoExtension.class)
class InvestmentServiceListingRuleTest {

    @Mock
    private InvestmentRepository investmentRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private InvestmentMapper investmentMapper;
    @Mock
    private InitialTransactionFactory initialTransactionFactory;
    @Mock
    private MarketDataAggregatorService marketDataAggregatorService;
    @Mock
    private InvestmentListingRules investmentListingRules;
    @Mock
    private BondCouponService bondCouponService;

    private InvestmentService investmentService;

    @BeforeEach
    void setUp() {
        investmentService = new InvestmentService(
                investmentRepository,
                transactionRepository,
                investmentMapper,
                initialTransactionFactory,
                marketDataAggregatorService,
                investmentListingRules,
                Mockito.mock(com.investment.tracker.config.CouponRules.class),
                bondCouponService);
    }

    private void stubEligibility(String type, boolean eligible) {
        Mockito.lenient().when(investmentListingRules.isAutoPriceEligible(type)).thenReturn(eligible);
        Mockito.lenient().when(investmentListingRules.isListedByDefault(type)).thenReturn(eligible);
    }

    private FDInvestmentDTO fdDto(Boolean isListed) {
        FDInvestmentDTO dto = new FDInvestmentDTO();
        dto.setType(com.investment.tracker.model.InvestmentType.FD);
        dto.setName("HDFC 5-year FD");
        dto.setCurrency("INR");
        dto.setPrincipal(new BigDecimal("100000"));
        dto.setIsListed(isListed);
        return dto;
    }

    private FDInvestment fdEntity(Boolean isListed) {
        FDInvestment entity = new FDInvestment();
        entity.setType(com.investment.tracker.model.InvestmentType.FD);
        entity.setName("HDFC 5-year FD");
        entity.setCurrency("INR");
        entity.setPrincipal(new BigDecimal("100000"));
        entity.setIsListed(isListed);
        entity.setPurchaseDate(java.time.LocalDate.of(2025, 4, 1));
        return entity;
    }

    @Test
    void createForcesListedFalseForIneligibleTypeEvenWhenClientSendsTrue() {
        stubEligibility("FD", false);
        when(investmentMapper.toEntity(any(), eq(42L))).thenAnswer(inv -> fdEntity(true));
        when(investmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(investmentMapper.toDTO(any())).thenAnswer(inv -> fdDto(false));

        investmentService.createInvestment(42L, fdDto(true));

        ArgumentCaptor<Investment> captor = ArgumentCaptor.forClass(Investment.class);
        verify(investmentRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsListed(),
                "FD must be coerced to is_listed=false server-side (INV-3)");
    }

    @Test
    void updateForcesListedFalseForIneligibleType() {
        stubEligibility("FD", false);
        FDInvestment existing = fdEntity(true); // legacy poisoned row
        when(investmentRepository.findByIdAndUserId(7L, 42L))
                .thenReturn(java.util.Optional.of(existing));
        when(investmentMapper.toEntity(any(), eq(42L))).thenAnswer(inv -> fdEntity(true));
        when(investmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        investmentService.updateInvestment(42L, 7L, fdDto(true));

        ArgumentCaptor<Investment> captor = ArgumentCaptor.forClass(Investment.class);
        verify(investmentRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsListed());
    }

    @Test
    void createKeepsListedTrueForEligibleType() {
        stubEligibility("EQUITY", true);
        com.investment.tracker.dto.EquityInvestmentDTO equityDto =
                new com.investment.tracker.dto.EquityInvestmentDTO();
        equityDto.setType(com.investment.tracker.model.InvestmentType.EQUITY);
        equityDto.setName("Reliance");
        equityDto.setCurrency("INR");
        equityDto.setSymbol("RELIANCE");
        equityDto.setIsListed(true);

        com.investment.tracker.model.EquityInvestment equityEntity =
                new com.investment.tracker.model.EquityInvestment();
        equityEntity.setType(com.investment.tracker.model.InvestmentType.EQUITY);
        equityEntity.setName("Reliance");
        equityEntity.setCurrency("INR");
        equityEntity.setSymbol("RELIANCE");
        equityEntity.setIsListed(true);
        equityEntity.setPurchaseDate(java.time.LocalDate.of(2025, 4, 1));

        when(investmentMapper.toEntity(any(), eq(42L))).thenReturn(equityEntity);
        when(investmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(investmentMapper.toDTO(any())).thenAnswer(inv -> {
            Investment inv0 = inv.getArgument(0);
            com.investment.tracker.dto.EquityInvestmentDTO out =
                    new com.investment.tracker.dto.EquityInvestmentDTO();
            out.setType(inv0.getType());
            out.setIsListed(inv0.getIsListed());
            return out;
        });

        InvestmentDTO result = investmentService.createInvestment(42L, equityDto);

        ArgumentCaptor<Investment> captor = ArgumentCaptor.forClass(Investment.class);
        verify(investmentRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsListed(),
                "listed EQUITY must keep is_listed=true");
        assertTrue(result.getIsListed());
    }

    @Test
    void ineligibleTypeIsNeverAutoPriced() {
        stubEligibility("FD", false);
        when(investmentMapper.toEntity(any(), eq(42L))).thenAnswer(inv -> fdEntity(true));
        when(investmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(investmentMapper.toDTO(any())).thenAnswer(inv -> fdDto(false));

        investmentService.createInvestment(42L, fdDto(true));

        verify(marketDataAggregatorService, never()).updateInvestmentValue(any());
    }

    @Test
    void cashAccountAlwaysUnlisted() {
        stubEligibility("CASH", false);
        CashInvestment cash = new CashInvestment();
        cash.setType(com.investment.tracker.model.InvestmentType.CASH);
        cash.setName("Savings");
        cash.setCurrency("INR");
        cash.setIsListed(true);
        cash.setPurchaseDate(java.time.LocalDate.of(2025, 4, 1));

        when(investmentMapper.toEntity(any(), eq(42L))).thenReturn(cash);
        when(investmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(investmentMapper.toDTO(any())).thenAnswer(inv -> {
            Investment inv0 = inv.getArgument(0);
            com.investment.tracker.dto.CashInvestmentDTO out =
                    new com.investment.tracker.dto.CashInvestmentDTO();
            out.setType(inv0.getType());
            out.setIsListed(inv0.getIsListed());
            return out;
        });

        investmentService.createInvestment(42L, new com.investment.tracker.dto.CashInvestmentDTO());

        ArgumentCaptor<Investment> captor = ArgumentCaptor.forClass(Investment.class);
        verify(investmentRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsListed());
    }
}
