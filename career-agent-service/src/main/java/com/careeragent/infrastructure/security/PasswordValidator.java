package com.careeragent.infrastructure.security;

import com.careeragent.api.exception.PasswordValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates password complexity requirements (length, uppercase, lowercase, digit).
 */
@Component
public class PasswordValidator {

    /**
     * Validates the password and throws PasswordValidationException if requirements are not met.
     */
    public void validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password.length() < 8 || password.length() > 128) {
            violations.add("Password must be between 8 and 128 characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            violations.add("Password must contain at least one uppercase letter");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            violations.add("Password must contain at least one lowercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            violations.add("Password must contain at least one digit");
        }

        if (!violations.isEmpty()) {
            throw new PasswordValidationException(violations);
        }
    }
}
