package com.investment.tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.TreeMap;

/**
 * E13 — bundled All-India monthly CPI index and deflation math ("today's
 * rupees").
 *
 * The index table ships as a classpath resource ({@code data/cpi-india-monthly.csv},
 * OECD MEI series IND.CPALTT01.IXOB.M — the OECD's MoSPI-sourced digitization of
 * the official Indian CPI) so real-growth figures are deterministic and work
 * offline. An optional API refresh re-fetches the same series from the DBnomics
 * mirror and replaces the in-memory table when the payload fully covers the
 * bundled span; any failure keeps the bundled table (this method never throws).
 *
 * Months beyond the table's end — and, symmetrically, before its start — are
 * extrapolated at the RBI 4% inflation-target midpoint, an explicitly labelled
 * assumption the API refresh replaces as soon as real data is available.
 */
@Service
@Slf4j
public class InflationService {

    static final String CPI_RESOURCE = "data/cpi-india-monthly.csv";

    /** RBI inflation-target midpoint (4% ± 2%): the extrapolation assumption. */
    static final BigDecimal RBI_TARGET_MIDPOINT = new BigDecimal("0.04");

    /** DBnomics mirror of the OECD MEI monthly CPI index for India (index 2015=100). */
    static final String DBNOMICS_URL =
            "https://api.db.nomics.world/v22/series/OECD/MEI/IND.CPALTT01.IXOB.M?observations=1&format=csv";

    static final String SOURCE_LABEL = "All-India monthly CPI (OECD MEI / MoSPI, index 2015=100)";

    static final String SERIES_NOTE =
            "The bundled OECD/MoSPI monthly series runs ~0.5%/yr above the official "
            + "CPI-C Combined series (MoSPI no longer publishes the 2012=100 monthly "
            + "table in machine-readable form after the 2024=100 rebase). Index ratios "
            + "are base-invariant, so deflation results do not depend on the base year.";

    private final RestTemplate restTemplate;

    @Value("${inflation.refresh-url:" + DBNOMICS_URL + "}")
    private String refreshUrl;

    private TreeMap<YearMonth, BigDecimal> indexByMonth = new TreeMap<>();
    private volatile boolean refreshedFromApi = false;

    public InflationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void loadBundledTable() {
        TreeMap<YearMonth, BigDecimal> parsed = parseBundledCsv();
        if (parsed.isEmpty()) {
            throw new IllegalStateException("Bundled CPI table has no rows: " + CPI_RESOURCE);
        }
        this.indexByMonth = parsed;
        log.info("Loaded bundled All-India CPI table: {} months ({} to {})",
                parsed.size(), parsed.firstKey(), parsed.lastKey());
    }

    private TreeMap<YearMonth, BigDecimal> parseBundledCsv() {
        TreeMap<YearMonth, BigDecimal> parsed = new TreeMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(CPI_RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("month,")) continue;
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                try {
                    parsed.put(YearMonth.parse(parts[0].trim()), new BigDecimal(parts[1].trim()));
                } catch (Exception ignored) {
                    // skip malformed rows defensively — the bundled file is hand-maintained
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Bundled CPI table unreadable: " + CPI_RESOURCE, e);
        }
        return parsed;
    }

    /**
     * Optional API refresh (the "bundled table + optional API refresh" model
     * agreed for E13): re-fetches the OECD MEI series from DBnomics and replaces
     * the in-memory index when the payload fully covers the bundled span.
     * Never throws — on any failure the bundled table stays authoritative.
     *
     * @return true when the live index was replaced by API data
     */
    public synchronized boolean refreshFromApi() {
        if (restTemplate == null) return false;
        String url = (refreshUrl == null || refreshUrl.trim().isEmpty()) ? DBNOMICS_URL : refreshUrl;
        try {
            String body = restTemplate.getForObject(url, String.class);
            TreeMap<YearMonth, BigDecimal> remote = parseRemoteCsv(body);
            if (remote.isEmpty()
                    || remote.firstKey().isAfter(indexByMonth.firstKey())
                    || remote.lastKey().isBefore(indexByMonth.lastKey())) {
                log.info("CPI refresh: remote series does not fully cover the bundled span "
                        + "— keeping bundled table (remote: {} to {})",
                        remote.isEmpty() ? "n/a" : remote.firstKey(),
                        remote.isEmpty() ? "n/a" : remote.lastKey());
                return false;
            }
            indexByMonth = remote;
            refreshedFromApi = true;
            log.info("CPI refresh: live index replaced ({} months, {} to {})",
                    remote.size(), remote.firstKey(), remote.lastKey());
            return true;
        } catch (Exception e) {
            log.warn("CPI refresh failed — bundled table kept ({}: {})",
                    e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /** Parses the DBnomics CSV ({@code period,value} rows; header and 'NA' values skipped). */
    static TreeMap<YearMonth, BigDecimal> parseRemoteCsv(String csv) {
        TreeMap<YearMonth, BigDecimal> out = new TreeMap<>();
        if (csv == null || csv.trim().isEmpty()) return out;
        for (String line : csv.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty() || !line.matches("\\d{4}-\\d{2},.*")) continue;
            String[] parts = line.split(",");
            if (parts.length < 2) continue;
            String value = parts[1].trim();
            if (value.equalsIgnoreCase("NA")) continue;
            try {
                out.put(YearMonth.parse(parts[0].trim()), new BigDecimal(value));
            } catch (Exception ignored) {
                // skip malformed rows
            }
        }
        return out;
    }

    /** True when the month has an official (bundled or API-refreshed) index value. */
    public boolean isCovered(YearMonth month) {
        return indexByMonth.containsKey(month);
    }

    /** Official index for a covered month. */
    public BigDecimal getIndex(YearMonth month) {
        BigDecimal value = indexByMonth.get(month);
        if (value == null) {
            throw new IllegalArgumentException("No CPI index for " + month + " — check isCovered() first");
        }
        return value;
    }

    /**
     * Index used for deflation: the official value for covered months; beyond
     * the table's end (or before its start) a projection at the RBI target
     * midpoint, compounded monthly.
     */
    public BigDecimal getEffectiveIndex(YearMonth month) {
        YearMonth first = indexByMonth.firstKey();
        YearMonth last = indexByMonth.lastKey();
        if (!month.isAfter(last) && !month.isBefore(first)) {
            return indexByMonth.get(month);
        }
        long months = month.isAfter(last)
                ? ChronoUnit.MONTHS.between(last, month)
                : ChronoUnit.MONTHS.between(first, month); // negative when before the table
        double growth = Math.pow(1 + RBI_TARGET_MIDPOINT.doubleValue(), months / 12.0);
        BigDecimal anchor = month.isAfter(last)
                ? indexByMonth.lastEntry().getValue()
                : indexByMonth.firstEntry().getValue();
        return anchor.multiply(BigDecimal.valueOf(growth)).setScale(4, RoundingMode.HALF_UP);
    }

    /** CPI ratio restating a value from {@code from}'s rupees into {@code to}'s rupees. */
    public BigDecimal getDeflator(YearMonth from, YearMonth to) {
        return getEffectiveIndex(to).divide(getEffectiveIndex(from), 6, RoundingMode.HALF_UP);
    }

    /** Restates {@code amount} (observed in {@code from}'s rupees) into {@code to}'s rupees. */
    public BigDecimal deflate(BigDecimal amount, YearMonth from, YearMonth to) {
        return amount.multiply(getDeflator(from, to)).setScale(2, RoundingMode.HALF_UP);
    }

    /** True when the month falls outside the official table's span (projection in use). */
    public boolean isExtrapolated(YearMonth month) {
        return month.isAfter(indexByMonth.lastKey()) || month.isBefore(indexByMonth.firstKey());
    }

    public boolean isRefreshedFromApi() {
        return refreshedFromApi;
    }

    public YearMonth getFirstCoveredMonth() {
        return indexByMonth.firstKey();
    }

    public YearMonth getLastCoveredMonth() {
        return indexByMonth.lastKey();
    }

    public int getCoveredMonthCount() {
        return indexByMonth.size();
    }
}
