package com.investment.tracker.service;

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
 * Parses bank statement PDFs into normalized transaction rows
 * (backlog feature F6). Pure parsing — no persistence, no account
 * resolution (that lives in {@link BankStatementImportService}).
 *
 * <p>Handles the two universal statement layouts without per-bank templates:</p>
 * <ul>
 *   <li><b>Marked amounts</b>: Cr/Dr suffix on the amount, e.g.
 *       {@code 01/05/2026 GROCERY STORE 4,500.00 Dr}</li>
 *   <li><b>Unmarked amounts</b>: direction inferred from an explicit minus
 *       sign, then from withdrawal/deposit keywords in the row
 *       (covers two-column layouts where only one of debit/credit is filled)</li>
 * </ul>
 *
 * <p>Wrapped rows are folded: a date-led line without an amount opens a
 * pending entry, and continuation lines (with or without the amount) complete
 * it. The raw PDF text/bytes are held only in memory and discarded.</p>
 */
@Component
@Slf4j
public class StatementPdfParser {

    /** Normalized transaction row parsed from a statement. */
    public static final class ParsedTxn {
        private final LocalDate date;
        private final String description;
        private final BigDecimal amount;   // always positive
        private final boolean credit;      // true = money in (Cr), false = money out (Dr)

        public ParsedTxn(LocalDate date, String description, BigDecimal amount, boolean credit) {
            this.date = date;
            this.description = description;
            this.amount = amount;
            this.credit = credit;
        }

        public LocalDate getDate() { return date; }
        public String getDescription() { return description; }
        public BigDecimal getAmount() { return amount; }
        public boolean isCredit() { return credit; }
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = Arrays.asList(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    /** Row starts with a date, then description/amount tokens to end of line. */
    private static final Pattern ROW_START = Pattern.compile(
            "^(\\d{1,2}[-/ ]\\d{1,2}[-/ ]\\d{2,4}|\\d{1,2}[- ][A-Za-z]{3,9}[- ]\\d{2,4}|[A-Za-z]{3,9} \\d{1,2}, \\d{4})\\s+(.*)$");

    /** Amount with optional thousands separators, optional sign, optional Dr/Cr marker. */
    private static final Pattern MONEY = Pattern.compile(
            "([+-]?)\\s*((?:\\d{1,3}(?:,\\d{2,3})*|\\d+)(?:\\.\\d{1,2})?)\\s*(Dr|Cr|DR|CR)?\\.?");
    private static final Pattern MONEY_ALL = Pattern.compile(MONEY.pattern(), Pattern.CASE_INSENSITIVE);

    private static final Pattern WITHDRAWAL_KEYWORD = Pattern.compile(
            "(?i)(withdrawal|debit|dr\\b|atm|purchase|payment\\s+to|transfer\\s+to|imps\\s*-|neft\\s*-|upi/|bill\\s*pay)");
    private static final Pattern DEPOSIT_KEYWORD = Pattern.compile(
            "(?i)(deposit|credit|cr\\b|salary|interest|refund|transfer\\s+from|dividend|cashback)");

    /**
     * Extracts the full statement text and parses all transaction rows.
     *
     * @return parsed rows (may be empty if nothing matched)
     * @throws IOException when the PDF cannot be read
     */
    public List<ParsedTxn> parse(byte[] pdfBytes) throws IOException {
        return parseText(extractText(pdfBytes));
    }

    /**
     * Extracts raw text from the PDF (used for bank detection and
     * parse-quality checks). In-memory only.
     */
    public String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /** Visible for testing: parse pre-extracted statement text. */
    public List<ParsedTxn> parseText(String text) {
        List<ParsedTxn> txns = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return txns;
        }

        // Pending (wrapped) entry: a date-led row whose amount hasn't appeared yet.
        LocalDate pendingDate = null;
        String pendingDescription = null;
        int parsedCount = 0;

        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Matcher rowMatcher = ROW_START.matcher(trimmed);
            if (!rowMatcher.matches()) {
                // Continuation line — completes a pending entry when it carries the amount.
                if (pendingDate != null) {
                    pendingDescription = (pendingDescription + " " + trimmed).trim();
                    MoneyToken token = lastMoneyToken(trimmed);
                    if (token != null) {
                        txns.add(emit(pendingDate, stripMoney(pendingDescription), token));
                        parsedCount++;
                        pendingDate = null;
                        pendingDescription = null;
                    }
                }
                continue;
            }

            LocalDate date = parseDate(rowMatcher.group(1));
            String remainder = rowMatcher.group(2).trim();
            if (date == null) {
                if (pendingDate != null) {
                    pendingDescription = (pendingDescription + " " + trimmed).trim();
                }
                continue;
            }

            MoneyToken token = lastMoneyToken(remainder);
            if (token == null) {
                // Date-led row whose amount sits on a following wrapped line.
                pendingDate = date;
                pendingDescription = remainder;
                continue;
            }

            String description = stripMoney(remainder);
            txns.add(emit(date, description, token));
            parsedCount++;
            pendingDate = null;
            pendingDescription = null;
        }

        log.debug("Statement parse: {} transaction(s) parsed.", parsedCount);
        return txns;
    }

    /**
     * Fraction of date-led lines that produced a transaction — used by the
     * import service to reject unrecognised statement formats.
     */
    public double parseRatio(String text) {
        if (text == null || text.isBlank()) return 0;
        int dateRows = 0;
        int txnRows = 0;
        for (String line : text.split("\\r?\\n")) {
            Matcher matcher = ROW_START.matcher(line.trim());
            if (matcher.matches()) {
                dateRows++;
                if (parseDate(matcher.group(1)) != null
                        && lastMoneyToken(matcher.group(2)) != null) {
                    txnRows++;
                }
            }
        }
        return dateRows == 0 ? 0 : (double) txnRows / dateRows;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static final class MoneyToken {
        final BigDecimal amount;
        final String marker;   // "Dr"/"Cr" or null
        final String sign;     // "", "+" or "-"

        MoneyToken(BigDecimal amount, String marker, String sign) {
            this.amount = amount;
            this.marker = marker;
            this.sign = sign;
        }
    }

    /** Last money token on the line (the amount column), or null when absent. */
    private MoneyToken lastMoneyToken(String text) {
        Matcher matcher = MONEY_ALL.matcher(text);
        MoneyToken token = null;
        while (matcher.find()) {
            token = new MoneyToken(
                    new BigDecimal(matcher.group(2).replace(",", "")),
                    matcher.group(3),
                    matcher.group(1));
        }
        return token;
    }

    /** Description = the row text with all money tokens stripped and collapsed. */
    private String stripMoney(String text) {
        String description = MONEY_ALL.matcher(text).replaceAll(" ")
                .replaceAll("\\s{2,}", " ").trim();
        return description.isEmpty() ? "Statement entry" : description;
    }

    private ParsedTxn emit(LocalDate date, String description, MoneyToken token) {
        boolean credit = resolveDirection(description, token);
        return new ParsedTxn(date, description, token.amount.abs(), credit);
    }

    /**
     * Direction resolution order: explicit Dr/Cr marker → explicit minus sign
     * → withdrawal/deposit keywords in the row → default debit (money out).
     */
    private boolean resolveDirection(String description, MoneyToken token) {
        if (token.marker != null) {
            return token.marker.equalsIgnoreCase("Cr");
        }
        if (token.amount.signum() < 0 || "-".equals(token.sign)) {
            return false;
        }
        if (WITHDRAWAL_KEYWORD.matcher(description).find()) {
            return false;
        }
        if (DEPOSIT_KEYWORD.matcher(description).find()) {
            return true;
        }
        return false; // ambiguous defaults to debit (money out)
    }

    private LocalDate parseDate(String raw) {
        String candidate = raw.trim().replaceAll("\\s{2,}", " ");
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(candidate, formatter);
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }
        return null;
    }
}
