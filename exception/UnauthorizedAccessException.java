package com.investment.tracker.exception;

/**
 * Exception thrown when a user tries to access a resource they don't own.
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException() {
        super("You are not authorized to access this resource");
    }
}
