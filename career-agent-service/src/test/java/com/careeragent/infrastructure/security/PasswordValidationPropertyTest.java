package com.careeragent.infrastructure.security;

import com.careeragent.api.exception.PasswordValidationException;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * **Validates: Requirements 12.6**
 */
@Label("Feature: career-agent, Property 18: Password Validation Rules")
class PasswordValidationPropertyTest {

    private final PasswordValidator validator = new PasswordValidator();

    // Property: Valid passwords are accepted
    @Property(tries = 200)
    @Label("Valid passwords (8-128 chars, uppercase, lowercase, digit) are accepted")
    void validPasswordsAreAccepted(@ForAll("validPasswords") String password) {
        assertThatCode(() -> validator.validate(password))
                .doesNotThrowAnyException();
    }

    // Property: Passwords shorter than 8 chars are rejected
    @Property(tries = 100)
    @Label("Passwords shorter than 8 characters are rejected")
    void tooShortPasswordsAreRejected(@ForAll("tooShortPasswords") String password) {
        assertThatThrownBy(() -> validator.validate(password))
                .isInstanceOf(PasswordValidationException.class);
    }

    // Property: Passwords longer than 128 chars are rejected
    @Property(tries = 100)
    @Label("Passwords longer than 128 characters are rejected")
    void tooLongPasswordsAreRejected(@ForAll("tooLongPasswords") String password) {
        assertThatThrownBy(() -> validator.validate(password))
                .isInstanceOf(PasswordValidationException.class);
    }

    // Property: Passwords without uppercase are rejected
    @Property(tries = 100)
    @Label("Passwords without uppercase letter are rejected")
    void noUppercasePasswordsAreRejected(@ForAll("noUppercasePasswords") String password) {
        assertThatThrownBy(() -> validator.validate(password))
                .isInstanceOf(PasswordValidationException.class);
    }

    // Property: Passwords without lowercase are rejected
    @Property(tries = 100)
    @Label("Passwords without lowercase letter are rejected")
    void noLowercasePasswordsAreRejected(@ForAll("noLowercasePasswords") String password) {
        assertThatThrownBy(() -> validator.validate(password))
                .isInstanceOf(PasswordValidationException.class);
    }

    // Property: Passwords without digit are rejected
    @Property(tries = 100)
    @Label("Passwords without digit are rejected")
    void noDigitPasswordsAreRejected(@ForAll("noDigitPasswords") String password) {
        assertThatThrownBy(() -> validator.validate(password))
                .isInstanceOf(PasswordValidationException.class);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(8)
                .ofMaxLength(128)
                .filter(s -> s.chars().anyMatch(Character::isUpperCase))
                .filter(s -> s.chars().anyMatch(Character::isLowerCase))
                .filter(s -> s.chars().anyMatch(Character::isDigit));
    }

    @Provide
    Arbitrary<String> tooShortPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(7)
                .filter(s -> s.chars().anyMatch(Character::isUpperCase))
                .filter(s -> s.chars().anyMatch(Character::isLowerCase))
                .filter(s -> s.chars().anyMatch(Character::isDigit));
    }

    @Provide
    Arbitrary<String> tooLongPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(129)
                .ofMaxLength(200)
                .filter(s -> s.chars().anyMatch(Character::isUpperCase))
                .filter(s -> s.chars().anyMatch(Character::isLowerCase))
                .filter(s -> s.chars().anyMatch(Character::isDigit));
    }

    @Provide
    Arbitrary<String> noUppercasePasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(8)
                .ofMaxLength(128)
                .filter(s -> s.chars().noneMatch(Character::isUpperCase))
                .filter(s -> s.chars().anyMatch(Character::isLowerCase))
                .filter(s -> s.chars().anyMatch(Character::isDigit));
    }

    @Provide
    Arbitrary<String> noLowercasePasswords() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(8)
                .ofMaxLength(128)
                .filter(s -> s.chars().anyMatch(Character::isUpperCase))
                .filter(s -> s.chars().noneMatch(Character::isLowerCase))
                .filter(s -> s.chars().anyMatch(Character::isDigit));
    }

    @Provide
    Arbitrary<String> noDigitPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .ofMinLength(8)
                .ofMaxLength(128)
                .filter(s -> s.chars().anyMatch(Character::isUpperCase))
                .filter(s -> s.chars().anyMatch(Character::isLowerCase))
                .filter(s -> s.chars().noneMatch(Character::isDigit));
    }
}
