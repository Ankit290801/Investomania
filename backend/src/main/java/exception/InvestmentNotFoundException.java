package com.investment.tracker.exception;

/**
 * Exception thrown when an investment is not found.
 */
public class InvestmentNotFoundException extends RuntimeException {
    
    public InvestmentNotFoundException(String message) {
        super(message);
    }
    
    public InvestmentNotFoundException(Long investmentId) {
        super("Investment not found with ID: " + investmentId);
    }
}
