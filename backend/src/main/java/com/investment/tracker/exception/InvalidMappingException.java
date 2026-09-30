package com.investment.tracker.exception;

/**
 * Exception thrown when expense mapping validation fails.
 */
public class InvalidMappingException extends RuntimeException {

    public InvalidMappingException(String message) {
        super(message);
    }
}
