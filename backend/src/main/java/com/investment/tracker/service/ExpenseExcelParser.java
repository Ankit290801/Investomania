package com.investment.tracker.service;

import com.investment.tracker.config.ExpenseImportRules;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an uploaded Excel (.xlsx) sheet into raw expense-import rows
 * (backlog feature F2). Pure parsing — no persistence, no validation
 * policy (that lives in {@link ExpenseImportService}).
 *
 * <p>Expected column order (header row optional but recommended):
 * Date | Description | Amount | Category (optional) | Currency (optional) |
 * Notes (optional).</p>
 *
 * <p>The stream is read fully in memory and never persisted — the raw file
 * is discarded when this method returns.</p>
 */
@Component
@RequiredArgsConstructor
public class ExpenseExcelParser {

    private final ExpenseImportRules importRules;
    private final DataFormatter formatter = new DataFormatter();

    /** Raw row as read from the sheet, before validation/normalisation. */
    public static final class RawRow {
        private final int rowNumber;
        private final LocalDate date;
        private final String description;
        private final BigDecimal amount;
        private final String category;
        private final String currency;
        private final String notes;

        public RawRow(int rowNumber, LocalDate date, String description,
                      BigDecimal amount, String category, String currency, String notes) {
            this.rowNumber = rowNumber;
            this.date = date;
            this.description = description;
            this.amount = amount;
            this.category = category;
            this.currency = currency;
            this.notes = notes;
        }

        public int rowNumber() { return rowNumber; }
        public LocalDate date() { return date; }
        public String description() { return description; }
        public BigDecimal amount() { return amount; }
        public String category() { return category; }
        public String currency() { return currency; }
        public String notes() { return notes; }
    }

    public List<RawRow> parse(MultipartFile file) throws IOException {
        List<RawRow> rows = new ArrayList<>();
        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int lastRow = sheet.getLastRowNum();
            if (lastRow + 1 > importRules.getMaxRows() + 1) { // +1 header
                throw new IllegalArgumentException(
                        "Sheet has more than " + importRules.getMaxRows() + " data rows");
            }

            for (int i = 1; i <= lastRow; i++) { // skip header row
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }
                rows.add(new RawRow(
                        i + 1,
                        readDate(row.getCell(0)),
                        readString(row.getCell(1)),
                        readAmount(row.getCell(2)),
                        readString(row.getCell(3)),
                        readString(row.getCell(4)),
                        readString(row.getCell(5))
                ));
            }
        }
        return rows;
    }

    private boolean isEmptyRow(Row row) {
        for (int c = 0; c < 6; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !(cell.getCellType() == CellType.STRING
                        && cell.getStringCellValue().trim().isEmpty())) {
                return false;
            }
        }
        return true;
    }

    private LocalDate readDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    LocalDateTime dt = cell.getLocalDateTimeCellValue();
                    return dt.toLocalDate();
                }
                return null; // numeric but not a date — invalid
            }
            String text = formatter.formatCellValue(cell).trim();
            if (text.isEmpty()) return null;
            return LocalDate.parse(text); // ISO yyyy-MM-dd
        } catch (Exception e) {
            return null; // unparseable dates are reported as ERROR rows
        }
    }

    private BigDecimal readAmount(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue())
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }
            String text = formatter.formatCellValue(cell).trim().replace(",", "");
            if (text.isEmpty()) return null;
            return new BigDecimal(text).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private String readString(Cell cell) {
        if (cell == null) return null;
        String text = formatter.formatCellValue(cell);
        return text != null && !text.trim().isEmpty() ? text.trim() : null;
    }
}
