package com.investment.tracker.service;

import com.investment.tracker.config.ContractNoteRules;
import com.investment.tracker.dto.ContractNoteImportReportDTO;
import com.investment.tracker.dto.ContractNoteImportReportDTO.ContractNoteTradeDTO;
import com.investment.tracker.model.CashInvestment;
import com.investment.tracker.model.EquityInvestment;
import com.investment.tracker.model.InvestmentType;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for contract note import portfolio semantics (Phase 9 / 8.3):
 * avg-price recompute, new-investment creation, settlement cash legs and
 * partial sells.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractNoteImportServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SnapshotGenerationService snapshotGenerationService;

    private ContractNoteImportService importService;

    private static final Long USER_ID = 42L;
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 21);

    private CashInvestment cash;
    private final List<Transaction> savedTxns = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // In-memory transaction store: save() appends, find...Desc() filters by
        // investment — so the service's recompute sees its own saved txns.
        ContractNoteRules rules = new ContractNoteRules(null, "test") {
            // Config values are set directly; load() would try to read a file.
            @Override
            public long getMaxFileKb() { return 2048; }
            @Override
            public int getMaxTrades() { return 500; }
            @Override
            public String getDefaultMarket() { return "NSE"; }
            @Override
            public String getDefaultCurrency() { return "INR"; }
            @Override
            public String cashAccountNameFor(String broker) { return broker + " Settlement Account"; }
        };
        importService = null; // tests construct per-case with a stub parser

        cash = CashInvestment.builder()
                .id(100L)
                .userId(USER_ID)
                .type(InvestmentType.CASH)
                .name("Zerodha Settlement Account")
                .bankName("Zerodha")
                .currency("INR")
                .currentValue(new BigDecimal("100000"))
                .purchaseDate(TRADE_DATE.minusDays(30))
                .isListed(false)
                .build();

        when(investmentRepository.findByUserId(USER_ID))
                .thenAnswer(inv -> new ArrayList<>(List.of(cash)));
        when(investmentRepository.save(any())).thenAnswer(inv -> {
            Object arg = inv.getArgument(0);
            if (arg instanceof EquityInvestment && ((EquityInvestment) arg).getId() == null) {
                ((EquityInvestment) arg).setId(900L);
            }
            return arg;
        });
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction txn = inv.getArgument(0);
            savedTxns.add(txn);
            return txn;
        });
        when(transactionRepository.findByInvestmentIdOrderByTransactionDateDesc(any()))
                .thenAnswer(inv -> {
                    Long investmentId = inv.getArgument(0);
                    return savedTxns.stream()
                            .filter(t -> investmentId.equals(t.getInvestmentId()))
                            .collect(java.util.stream.Collectors.toList());
                });
    }

    private EquityInvestment holding(String symbol, BigDecimal qty, BigDecimal avg) {
        EquityInvestment equity = EquityInvestment.builder()
                .id(200L)
                .userId(USER_ID)
                .type(InvestmentType.EQUITY)
                .name(symbol)
                .symbol(symbol)
                .market("NSE")
                .currency("INR")
                .quantity(qty)
                .avgPrice(avg)
                .purchaseDate(TRADE_DATE.minusDays(30))
                .isListed(true)
                .build();
        when(investmentRepository.findByUserId(USER_ID))
                .thenAnswer(inv -> new ArrayList<>(List.of(cash, equity)));
        return equity;
    }

    private ContractNotePdfParser.ParsedTrade trade(String symbol, boolean buy,
                                                    String qty, String price) {
        return new ContractNotePdfParser.ParsedTrade(
                symbol, buy, new BigDecimal(qty), new BigDecimal(price), TRADE_DATE);
    }

    /** Parser stub returning fixed trades from any text. */
    private ContractNotePdfParser parserReturning(List<ContractNotePdfParser.ParsedTrade> trades) {
        return new ContractNotePdfParser(null) {
            @Override
            public java.util.List<ParsedTrade> parseText(String text) {
                return trades;
            }

            @Override
            public String extractText(byte[] bytes) {
                return "zerodha contract note";
            }
        };
    }

    @Test
    void newStockBuyCreatesInvestmentAndCashDebit() throws Exception {
        ContractNoteImportService service = new ContractNoteImportService(
                rulesWith(), parserReturning(List.of(trade("RELIANCE", true, "10", "2850.00"))),
                investmentRepository, transactionRepository, snapshotGenerationService);

        ContractNoteImportReportDTO report = service.commit(USER_ID, multipart());

        assertEquals(1, report.getValidTrades());
        assertEquals(ContractNoteTradeDTO.STATUS_NEW, report.getTrades().get(0).getStatus());
        assertEquals(0, new BigDecimal("-28500.00").compareTo(report.getNetCashImpact()));

        // Investment created with symbol + market + purchase date
        ArgumentCaptor<com.investment.tracker.model.Investment> invCaptor =
                ArgumentCaptor.forClass(com.investment.tracker.model.Investment.class);
        verify(investmentRepository, atLeastOnce()).save(invCaptor.capture());
        boolean created = invCaptor.getAllValues().stream()
                .anyMatch(i -> i instanceof EquityInvestment
                        && "RELIANCE".equals(((EquityInvestment) i).getSymbol())
                        && "NSE".equals(((EquityInvestment) i).getMarket())
                        && TRADE_DATE.equals(i.getPurchaseDate()));
        assertTrue(created, "new EQUITY investment must be created for the CNOTE stock");

        // Cash leg: TRANSFER_OUT of 28500
        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeastOnce()).save(txnCaptor.capture());
        assertTrue(txnCaptor.getAllValues().stream().anyMatch(t ->
                        t.getType() == TransactionType.TRANSFER_OUT
                                && 0 == new BigDecimal("28500.00").compareTo(t.getAmount())
                                && t.getInvestmentId().equals(100L)),
                "cash debit (TRANSFER_OUT) must be booked on the settlement account");
    }

    @Test
    void existingHoldingBuyRecomputesWeightedAveragePrice() throws Exception {
        EquityInvestment tcs = holding("TCS", new BigDecimal("5"), new BigDecimal("4000.0000"));

        // Prior history: 5 @ 4000 = 20,000. New BUY: 5 @ 4400 = 22,000.
        // Expected: qty 10, avg (20000+22000)/10 = 4200.
        savedTxns.add(Transaction.builder()
                .investmentId(200L).type(TransactionType.BUY)
                .quantity(new BigDecimal("5")).amount(new BigDecimal("20000"))
                .transactionDate(TRADE_DATE.minusDays(30)).currency("INR").build());

        ContractNoteImportService service = new ContractNoteImportService(
                rulesWith(), parserReturning(List.of(trade("TCS", true, "5", "4400.00"))),
                investmentRepository, transactionRepository, snapshotGenerationService);

        ContractNoteImportReportDTO report = service.commit(USER_ID, multipart());

        assertEquals(ContractNoteTradeDTO.STATUS_UPDATED, report.getTrades().get(0).getStatus());
        ArgumentCaptor<com.investment.tracker.model.Investment> invCaptor =
                ArgumentCaptor.forClass(com.investment.tracker.model.Investment.class);
        verify(investmentRepository, atLeastOnce()).save(invCaptor.capture());
        EquityInvestment saved = invCaptor.getAllValues().stream()
                .filter(i -> i instanceof EquityInvestment && "TCS".equals(((EquityInvestment) i).getSymbol()))
                .map(i -> (EquityInvestment) i)
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("10").compareTo(saved.getQuantity()));
        assertEquals(0, new BigDecimal("4200.0000").compareTo(saved.getAvgPrice()));
    }

    @Test
    void partialSellKeepsRemainingQuantityAtSameAvgPrice() throws Exception {
        EquityInvestment infy = holding("INFY", new BigDecimal("25"), new BigDecimal("1500.0000"));

        // Prior history: buys 25 @ 1500 = 37,500 (no prior sells).
        savedTxns.add(Transaction.builder()
                .investmentId(200L).type(TransactionType.BUY)
                .quantity(new BigDecimal("25")).amount(new BigDecimal("37500"))
                .transactionDate(TRADE_DATE.minusDays(30)).currency("INR").build());

        ContractNoteImportService service = new ContractNoteImportService(
                rulesWith(), parserReturning(List.of(trade("INFY", false, "10", "1600.00"))),
                investmentRepository, transactionRepository, snapshotGenerationService);

        ContractNoteImportReportDTO report = service.commit(USER_ID, multipart());

        assertEquals(ContractNoteTradeDTO.STATUS_UPDATED, report.getTrades().get(0).getStatus());
        assertEquals(0, new BigDecimal("16000.00").compareTo(report.getNetCashImpact()),
                "sell must be a cash inflow");

        ArgumentCaptor<com.investment.tracker.model.Investment> invCaptor =
                ArgumentCaptor.forClass(com.investment.tracker.model.Investment.class);
        verify(investmentRepository, atLeastOnce()).save(invCaptor.capture());
        EquityInvestment saved = invCaptor.getAllValues().stream()
                .filter(i -> i instanceof EquityInvestment && "INFY".equals(((EquityInvestment) i).getSymbol()))
                .map(i -> (EquityInvestment) i)
                .findFirst().orElseThrow();
        // Remaining 15 units at the unchanged 1500 average.
        assertEquals(0, new BigDecimal("15").compareTo(saved.getQuantity()));
        assertEquals(0, new BigDecimal("1500.0000").compareTo(saved.getAvgPrice()));
    }

    @Test
    void oversellIsRejected() throws Exception {
        holding("WIPRO", new BigDecimal("5"), new BigDecimal("300.0000"));

        ContractNoteImportService service = new ContractNoteImportService(
                rulesWith(), parserReturning(List.of(trade("WIPRO", false, "10", "300.00"))),
                investmentRepository, transactionRepository, snapshotGenerationService);

        ContractNoteImportReportDTO report = service.commit(USER_ID, multipart());

        assertEquals(ContractNoteTradeDTO.STATUS_ERROR, report.getTrades().get(0).getStatus());
        assertTrue(report.getTrades().get(0).getMessage().contains("only 5 held"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ContractNoteRules rulesWith() {
        return new ContractNoteRules(null, "test") {
            @Override
            public long getMaxFileKb() { return 2048; }
            @Override
            public int getMaxTrades() { return 500; }
            @Override
            public String getDefaultMarket() { return "NSE"; }
            @Override
            public String getDefaultCurrency() { return "INR"; }
            @Override
            public String cashAccountNameFor(String broker) { return broker + " Settlement Account"; }
        };
    }

    private org.springframework.web.multipart.MultipartFile multipart() {
        return new org.springframework.mock.web.MockMultipartFile(
                "file", "cnote.pdf", "application/pdf", "zerodha contract note".getBytes());
    }
}
