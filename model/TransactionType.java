package com.investment.tracker.model;

/**
 * Enum representing different types of transactions.
 */
public enum TransactionType {
    BUY,           // Purchase of investment
    SELL,          // Sale of investment
    DIVIDEND,      // Dividend received from equity
    INTEREST,      // Interest received from FD/RD/PPF/NPS
    CONTRIBUTION,  // Regular contribution to NPS/PPF/RD
    WITHDRAWAL,    // Withdrawal from investment
    BONUS,         // Bonus shares/units
    SPLIT,         // Stock split
    MERGER,        // Merger/acquisition adjustment
    SALARY,        // Salary income (credit to cash account)
    DEPOSIT,       // Cash deposit to bank account
    TRANSFER_IN,   // Transfer from another account
    TRANSFER_OUT   // Transfer to another account or investment
}
