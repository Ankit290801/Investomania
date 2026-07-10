package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entity representing symbol mappings between user input and API-specific symbols
 * This is the key to handling different symbol formats across Yahoo Finance, Google Finance, etc.
 */
@Entity
@Table(name = "symbol_mappings", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_symbol", columnNames = "user_symbol")
    },
    indexes = {
        @Index(name = "idx_yahoo_symbol", columnList = "yahoo_symbol"),
        @Index(name = "idx_google_symbol", columnList = "google_symbol"),
        @Index(name = "idx_market", columnList = "market")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User's input symbol (e.g., "RELIANCE", "AAPL")
     */
    @NotNull
    @Column(name = "user_symbol", nullable = false, unique = true, length = 50)
    private String userSymbol;
    
    /**
     * Yahoo Finance formatted symbol (e.g., "RELIANCE.NS", "AAPL")
     */
    @Column(name = "yahoo_symbol", length = 50)
    private String yahooSymbol;
    
    /**
     * Google Finance formatted symbol (e.g., "NSE:RELIANCE", "NASDAQ:AAPL")
     */
    @Column(name = "google_symbol", length = 50)
    private String googleSymbol;
    
    /**
     * Stock exchange/market
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Market market;
    
    /**
     * Investment type this symbol belongs to (EQUITY, CRYPTO)
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private InvestmentType assetType;
    
    /**
     * Whether this mapping has been verified with actual API calls
     */
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;
    
    /**
     * Last time this mapping was verified/validated
     */
    @Column(name = "last_verified")
    private LocalDateTime lastVerified;
    
    /**
     * Company/security name for display purposes
     */
    @Column(length = 200)
    private String name;
    
    /**
     * ISIN code (if available)
     */
    @Column(length = 12)
    private String isin;
    
    /**
     * Additional notes or metadata
     */
    @Column(length = 500)
    private String notes;
    
    /**
     * Timestamp when this mapping was created
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this mapping was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Mark this mapping as verified
     */
    public void markAsVerified() {
        this.isVerified = true;
        this.lastVerified = LocalDateTime.now();
    }
}
