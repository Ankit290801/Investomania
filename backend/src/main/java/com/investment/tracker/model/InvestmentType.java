package com.investment.tracker.model;

/**
 * Enum representing different types of investments supported by the system.
 * Each type must have a corresponding entity subclass of {@link Investment}
 * (SINGLE_TABLE inheritance) and a DTO + mapper pair in {@code dto}/{@code util}.
 */
public enum InvestmentType {
    EQUITY,           // Public equity (stocks)
    BOND,             // Bonds and debentures
    FD,               // Fixed Deposits
    RD,               // Recurring Deposits
    NPS,              // National Pension Scheme
    PPF,              // Public Provident Fund
    REAL_ESTATE,      // Real estate properties
    CRYPTO,           // Cryptocurrency
    CASH              // Cash in bank/savings account
}
