package com.investment.tracker.model;

/**
 * Status of a portfolio snapshot.
 */
public enum SnapshotStatus {
    CALCULATED,       // Fully calculated from market/interest data
    PARTIAL,          // Some assets have missing data (estimated or skipped)
    MANUAL_OVERRIDE   // User manually entered or overrode values
}
