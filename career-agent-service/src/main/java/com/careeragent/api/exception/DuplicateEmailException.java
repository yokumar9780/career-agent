package com.careeragent.api.exception;

/**
 * Thrown when a registration attempt uses an email that already exists.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
