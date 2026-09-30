package com.investment.tracker.service;

import com.investment.tracker.config.ContractNoteRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses broker contract note (CNOTE) PDFs into trade rows
 * (Phase 9 / Task 8.3). Pure parsing — no persistence, no portfolio updates
 * (that lives in {@link ContractNoteImportService}).
 *
 * <p>The trade-line shape is a per-broker regex template in
 * {@code contract-note.yml} with named groups (symbol, side, qty, price);
 * broker detection uses marker regexes with a Generic fallback. Adding a
 * broker is a config-only change.</p>
 *
 * <p>Raw PDF bytes are held only in memory and discarded.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractNotePdfParser {

    /** One trade parsed from a contract note. */
    public static final class ParsedTrade {
        private final String symbol;
        private final boolean buy;
        private final BigDecimal quantity;
        private final BigDecimal price;   // per unit
        private final LocalDate tradeDate;

        public ParsedTrade(String symbol, boolean buy, BigDecimal quantity,
                           BigDecimal price, LocalDate tradeDate) {
            this.symbol = symbol;
            this.buy = buy;
            this.quantity = quantity;
            this.price = price;
            this.tradeDate = tradeDate;
        }

        public String getSymbol() { return symbol; }
        public boolean isBuy() { return buy; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getPrice() { return price; }
        public LocalDate getTradeDate() { return tradeDate; }
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = Arrays.asList(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    private final ContractNoteRules contractNoteRules;

    /**
     * Extracts text, resolves the broker template and parses trade rows.
     *
     * @return parsed trades (may be empty if the template matched nothing)
     * @throws IOException when the PDF cannot be read
     */
    public List<ParsedTrade> parse(byte[] pdfBytes) throws IOException {
        return parseText(extractText(pdfBytes));
    }

    /** Extracts raw text from the PDF (broker detection + date scan). In-memory only. */
    public String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /** Visible for testing: parse pre-extracted contract note text. */
    public List<ParsedTrade> parseText(String text) {
        List<ParsedTrade> trades = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return trades;
        }

        ContractNoteRules.BrokerTemplate template = contractNoteRules.resolveTemplate(text);
        if (template == null) {
            log.warn("No contract note template available (broker markers unmatched, no Generic).");
            return trades;
        }

        LocalDate noteDate = detectNoteDate(text);
        Pattern tradeRow = template.getTradeRow();

        for (String line : text.split("\\r?\\n")) {
            Matcher matcher = tradeRow.matcher(line.trim());
            if (!matcher.matches()) {
                continue;
            }
            try {
                String symbol = matcher.group("symbol").trim().toUpperCase();
                boolean buy = matcher.group("side").trim().toUpperCase().startsWith("B");
                BigDecimal qty = new BigDecimal(matcher.group("qty").replace(",", ""));
                BigDecimal price = new BigDecimal(matcher.group("price").replace(",", ""));

                if (qty.signum() <= 0 || price.signum() <= 0 || symbol.isEmpty()) {
                    log.debug("Skipping degenerate trade row: {}", line);
                    continue;
                }
                trades.add(new ParsedTrade(symbol, buy, qty, price, noteDate));
            } catch (IllegalArgumentException e) {
                log.debug("Skipping unparseable trade row '{}': {}", line, e.getMessage());
            }
        }

        log.debug("Contract note parse ({}): {} trade(s).", template.getName(), trades.size());
        return trades;
    }

    /**
     * The contract note date: the first parseable date found in the document
     * header region (first 40 lines). Contract notes are issued for a single
     * trading day, so one date covers all trades in the note.
     */
    private LocalDate detectNoteDate(String text) {
        String[] lines = text.split("\\r?\\n");
        int limit = Math.min(lines.length, 40);
        for (int i = 0; i < limit; i++) {
            for (DateTimeFormatter formatter : DATE_FORMATS) {
                Matcher matcher = Pattern.compile("\\d{1,2}[-/ ][A-Za-z0-9]{2,9}[-/ ]\\d{2,4}|\\d{4}-\\d{2}-\\d{2}")
                        .matcher(lines[i]);
                while (matcher.find()) {
                    try {
                        return LocalDate.parse(matcher.group().trim(), formatter);
                    } catch (DateTimeParseException ignored) {
                        // try next pattern / match
                    }
                }
            }
        }
        return LocalDate.now();
    }
}
