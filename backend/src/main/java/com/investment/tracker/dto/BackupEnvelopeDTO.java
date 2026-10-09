package com.investment.tracker.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.investment.tracker.model.AssetValuation;
import com.investment.tracker.model.ExpenseAssetMapping;
import com.investment.tracker.model.PortfolioSnapshot;
import com.investment.tracker.model.SymbolMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Phase 9 Task 8.4 — envelope for the full-user-data JSON backup.
 *
 * <p>{@code schemaVersion} guards compatibility: restore refuses files whose
 * major version is newer than this build understands. {@code counts} lets
 * the client show what a backup contains before restoring, and serves as a
 * post-restore integrity check (restored row counts must equal the
 * envelope's counts).</p>
 *
 * <p>Deliberately excluded: {@code User} rows and password hashes (restore
 * targets the signed-in account; identities are not portable), and
 * derived/cache tables that rebuild themselves (market prices, price update
 * results). Everything a user entered is included.</p>
 *
 * <p>Investments round-trip polymorphically: {@code @JsonTypeInfo} embeds
 * the concrete subclass (EquityInvestmentDTO, BondInvestmentDTO, …) in each
 * element so Jackson deserialises back to the right type.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupEnvelopeDTO {

    /** Bump on structural change; restore rejects major > supported. */
    public static final int SCHEMA_VERSION = 1;
    /** Lower bound of schema versions this build can restore. */
    public static final int MIN_SUPPORTED_VERSION = 1;

    private int schemaVersion;

    /** Backup creation timestamp (ISO-8601). */
    private String createdAt;

    /** Username the data was exported from (informational). */
    private String exportedFor;

    /** Per-entity row counts for preview and post-restore verification. */
    private Map<String, Long> counts;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "dtoType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = EquityInvestmentDTO.class, name = "EQUITY"),
            @JsonSubTypes.Type(value = BondInvestmentDTO.class, name = "BOND"),
            @JsonSubTypes.Type(value = CashInvestmentDTO.class, name = "CASH"),
            @JsonSubTypes.Type(value = FDInvestmentDTO.class, name = "FD"),
            @JsonSubTypes.Type(value = RDInvestmentDTO.class, name = "RD"),
            @JsonSubTypes.Type(value = NPSInvestmentDTO.class, name = "NPS"),
            @JsonSubTypes.Type(value = PPFInvestmentDTO.class, name = "PPF"),
            @JsonSubTypes.Type(value = CryptoInvestmentDTO.class, name = "CRYPTO"),
            @JsonSubTypes.Type(value = RealEstateInvestmentDTO.class, name = "REAL_ESTATE")
    })
    private List<InvestmentDTO> investments;

    private List<TransactionDTO> transactions;
    private List<ExpenseDTO> expenses;
    /**
     * Expense→asset coverage links (E-mapping rows). These are user data and
     * must round-trip: the first restore implementation silently dropped
     * them, destroying all coverage mappings.
     */
    private List<ExpenseAssetMapping> expenseAssetMappings;
    /** Snapshot/valuation/mapping entities are DTO-less — serialised directly (simple, flat rows). */
    private List<PortfolioSnapshot> snapshots;
    private List<UserSettingsDTO> settings;
    private List<AssetValuation> assetValuations;
    private List<SymbolMapping> symbolMappings;
}
