package com.investment.tracker.service;

import com.investment.tracker.config.ContractNoteRules;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the contract note PDF parser (Phase 9 / Task 8.3).
 */
class ContractNotePdfParserTest {

    private final ContractNotePdfParser parser = new ContractNotePdfParser(contractRules());

    private static final String ZERODHA_NOTE = String.join("\n",
            "Zerodha Broking Ltd - Contract Note",
            "Trade Date: 21/09/2026",
            "Trades:",
            "1  RELIANCE  EQ  Buy   10  2,850.00  28,500.00",
            "2  TCS       EQ  Sell   5  4,120.00  20,600.00",
            "Totals: 49,100.00");

    private static final String GROWW_NOTE = String.join("\n",
            "Groww Invest Tech Pvt Ltd - Contract Note",
            "Date: 22-09-2026",
            "INFY  BUY   25  1,540.50  38,512.50",
            "HDFC  SELL  12    2,980.00  35,760.00");

    private static ContractNoteRules contractRules() {
        // Load the real bundled templates from the classpath.
        ContractNoteRules rules = new ContractNoteRules(
                new org.springframework.core.io.DefaultResourceLoader(),
                "classpath:config/contract-note.yml");
        rules.load();
        return rules;
    }

    @Test
    void parsesZerodhaTradeLinesWithBuySellSides() {
        List<ContractNotePdfParser.ParsedTrade> trades = parser.parseText(ZERODHA_NOTE);

        assertEquals(2, trades.size());

        ContractNotePdfParser.ParsedTrade reliance = trades.get(0);
        assertEquals("RELIANCE", reliance.getSymbol());
        assertTrue(reliance.isBuy());
        assertEquals(0, new BigDecimal("10").compareTo(reliance.getQuantity()));
        assertEquals(0, new BigDecimal("2850.00").compareTo(reliance.getPrice()));
        assertEquals(LocalDate.of(2026, 9, 21), reliance.getTradeDate());

        ContractNotePdfParser.ParsedTrade tcs = trades.get(1);
        assertEquals("TCS", tcs.getSymbol());
        assertTrue(!tcs.isBuy());
        assertEquals(0, new BigDecimal("5").compareTo(tcs.getQuantity()));
    }

    @Test
    void parsesGrowwTemplateLines() {
        List<ContractNotePdfParser.ParsedTrade> trades = parser.parseText(GROWW_NOTE);

        assertEquals(2, trades.size());
        assertEquals("INFY", trades.get(0).getSymbol());
        assertTrue(trades.get(0).isBuy());
        assertEquals("HDFC", trades.get(1).getSymbol());
        assertTrue(!trades.get(1).isBuy());
        assertEquals(LocalDate.of(2026, 9, 22), trades.get(1).getTradeDate());
    }

    @Test
    void ignoresNonTradeLines() {
        List<ContractNotePdfParser.ParsedTrade> trades = parser.parseText(
                "Some header\nNo trades in this random document\n100 plain words here");
        assertEquals(0, trades.size());
    }

    @Test
    void parsesRealGeneratedPdfBytes() throws IOException {
        byte[] pdf = generatePdf(ZERODHA_NOTE);
        List<ContractNotePdfParser.ParsedTrade> trades = parser.parse(pdf);

        assertEquals(2, trades.size());
        assertEquals(0, new BigDecimal("28500").compareTo(
                trades.get(0).getQuantity().multiply(trades.get(0).getPrice())));
    }

    /** Generates a real one-page PDF containing the given text (PDFBox). */
    private byte[] generatePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                stream.setFont(font, 10);
                float y = 780;
                for (String line : text.split("\n")) {
                    stream.beginText();
                    stream.newLineAtOffset(40, y);
                    stream.showText(line);
                    stream.endText();
                    y -= 14;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
