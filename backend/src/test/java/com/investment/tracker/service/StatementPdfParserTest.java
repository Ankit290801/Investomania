package com.investment.tracker.service;

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
 * Unit tests for the bank statement PDF parser (backlog feature F6).
 */
class StatementPdfParserTest {

    private final StatementPdfParser parser = new StatementPdfParser();

    private static final String SIGNED_TEXT = String.join("\n",
            "HDFC Bank - Statement of Account",
            "Account No: 1234567890",
            "Date        Description               Amount",
            "01/05/2026  SALARY MAY                1,20,000.00 Cr",
            "03/05/2026  GROCERY STORE UPI           4,500.00 Dr",
            "05/05/2026  ATM WITHDRAWAL MG ROAD      2,000.00 Dr",
            "10/05/2026  INTEREST PAID                 312.45 Cr");

    private static final String TWOCOL_TEXT = String.join("\n",
            "State Bank of India - Account Statement",
            "Date        Narration             Debit      Credit",
            "01/06/2026  NEFT SALARY                    1,50,000.00",
            "02/06/2026  BILL PAY ELECTRICITY  3,200.00",
            "04/06/2026  UPI/RESTAURANT          850.50");

    @Test
    void parsesCrDrSuffixedRows() {
        List<StatementPdfParser.ParsedTxn> txns = parser.parseText(SIGNED_TEXT);

        assertEquals(4, txns.size());

        StatementPdfParser.ParsedTxn salary = txns.get(0);
        assertEquals(LocalDate.of(2026, 5, 1), salary.getDate());
        assertEquals(new BigDecimal("120000.00"), salary.getAmount());
        assertTrue(salary.isCredit(), "Cr marker must map to credit");
        assertTrue(salary.getDescription().toLowerCase().contains("salary"));

        StatementPdfParser.ParsedTxn grocery = txns.get(1);
        assertEquals(new BigDecimal("4500.00"), grocery.getAmount());
        assertTrue(!grocery.isCredit(), "Dr marker must map to debit");
    }

    @Test
    void parsesTwoColumnLayoutWithKeywordDirection() {
        List<StatementPdfParser.ParsedTxn> txns = parser.parseText(TWOCOL_TEXT);

        assertEquals(3, txns.size());

        StatementPdfParser.ParsedTxn salary = txns.get(0);
        assertEquals(new BigDecimal("150000.00"), salary.getAmount());
        assertTrue(salary.isCredit(), "row with only a credit-column value is a credit");

        StatementPdfParser.ParsedTxn electricity = txns.get(1);
        assertEquals(new BigDecimal("3200.00"), electricity.getAmount());
        assertTrue(!electricity.isCredit(), "row with only a debit-column value is a debit");

        StatementPdfParser.ParsedTxn upi = txns.get(2);
        assertEquals(new BigDecimal("850.50"), upi.getAmount());
        assertTrue(!upi.isCredit());
    }

    @Test
    void foldsWrappedDescriptionLines() {
        String text = String.join("\n",
                "01/07/2026  UPI/PAYMENT/GOOGLE",
                "  PAYTO:bookstore IND",
                "                1,250.00 Dr");
        List<StatementPdfParser.ParsedTxn> txns = parser.parseText(text);

        assertEquals(1, txns.size());
        assertTrue(txns.get(0).getDescription().contains("GOOGLE")
                && txns.get(0).getDescription().toLowerCase().contains("bookstore"),
                "wrapped lines must fold into one description");
        assertEquals(0, new BigDecimal("1250.00").compareTo(txns.get(0).getAmount()));
    }

    @Test
    void parseRatioDetectsUnrecognisedFormats() {
        assertTrue(parser.parseRatio(SIGNED_TEXT) > 0.9);
        assertEquals(0, parser.parseRatio("This is not a bank statement.\nNo dates here."), 1e-9);
    }

    @Test
    void parsesRealGeneratedPdfBytes() throws IOException {
        byte[] pdf = generateStatementPdf(SIGNED_TEXT);
        List<StatementPdfParser.ParsedTxn> txns = parser.parse(pdf);

        assertEquals(4, txns.size());
        assertEquals(new BigDecimal("312.45"), txns.get(3).getAmount());
        assertTrue(txns.get(3).isCredit());
    }

    /** Generates a real one-page PDF containing the given text (PDFBox). */
    private byte[] generateStatementPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                stream.setFont(font, 10);
                float y = 750;
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
