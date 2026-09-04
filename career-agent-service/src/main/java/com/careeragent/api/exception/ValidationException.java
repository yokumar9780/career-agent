package com.careeragent.api.exception;

/**
 * Thrown when input validation fails for a request.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
