package com.investment.tracker.exception;

/**
 * Thrown when authentication credentials are invalid (login or
 * password-change verification).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
