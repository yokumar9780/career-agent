package com.careeragent.api.exception;

import java.util.List;

public class PasswordValidationException extends RuntimeException {

    private final List<String> violations;

    public PasswordValidationException(List<String> violations) {
        super("Password does not meet complexity requirements");
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
