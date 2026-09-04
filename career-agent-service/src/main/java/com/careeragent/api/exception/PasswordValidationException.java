package com.careeragent.api.exception;

import lombok.Getter;

import java.util.List;

/**
 * Thrown when a password does not meet complexity requirements.
 */
@Getter
public class PasswordValidationException extends RuntimeException {

    private final List<String> violations;

    public PasswordValidationException(List<String> violations) {
        super("Password does not meet complexity requirements");
        this.violations = violations;
    }
}
