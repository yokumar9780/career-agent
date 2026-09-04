package com.careeragent.service;

import com.careeragent.domain.RemotePreference;
import com.careeragent.domain.SeniorityLevel;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * **Validates: Requirements 2.2**
 */
@Label("Feature: career-agent, Property: Enum Validation Rules")
class EnumValidationPropertyTest {

    private final ValidationService validationService = new ValidationService();

    // --- Seniority Level ---

    @Property(tries = 200)
    @Label("Valid seniority levels (case-insensitive) are parsed correctly")
    void validSeniorityLevelsAreParsed(@ForAll("validSeniorityStrings") String input) {
        SeniorityLevel result = validationService.validateSeniorityLevel(input);
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(input.toUpperCase().trim());
    }

    @Property(tries = 200)
    @Label("Invalid seniority level strings throw IllegalArgumentException")
    void invalidSeniorityLevelsAreRejected(@ForAll("invalidSeniorityStrings") String input) {
        assertThatThrownBy(() -> validationService.validateSeniorityLevel(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid seniority level");
    }

    @Property(tries = 100)
    @Label("Null or blank seniority level returns null")
    void nullOrBlankSeniorityReturnsNull(@ForAll("nullOrBlankStrings") String input) {
        assertThat(validationService.validateSeniorityLevel(input)).isNull();
    }

    // --- Remote Preference ---

    @Property(tries = 200)
    @Label("Valid remote preferences (case-insensitive) are parsed correctly")
    void validRemotePreferencesAreParsed(@ForAll("validRemoteStrings") String input) {
        RemotePreference result = validationService.validateRemotePreference(input);
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(input.toUpperCase().trim());
    }

    @Property(tries = 200)
    @Label("Invalid remote preference strings throw IllegalArgumentException")
    void invalidRemotePreferencesAreRejected(@ForAll("invalidRemoteStrings") String input) {
        assertThatThrownBy(() -> validationService.validateRemotePreference(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid remote preference");
    }

    @Property(tries = 100)
    @Label("Null or blank remote preference returns ANY")
    void nullOrBlankRemoteReturnsAny(@ForAll("nullOrBlankStrings") String input) {
        assertThat(validationService.validateRemotePreference(input)).isEqualTo(RemotePreference.ANY);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> validSeniorityStrings() {
        Arbitrary<String> values = Arbitraries.of("INTERN", "JUNIOR", "MID", "SENIOR", "LEAD", "EXECUTIVE");
        Arbitrary<String> caseVariants = values.flatMap(v ->
                Arbitraries.of(v.toLowerCase(), v.toUpperCase(), capitalize(v), " " + v + " "));
        return caseVariants;
    }

    @Provide
    Arbitrary<String> invalidSeniorityStrings() {
        return Arbitraries.of(
                "BEGINNER", "EXPERT", "PRINCIPAL", "STAFF", "ASSOCIATE",
                "MANAGER", "DIRECTOR", "VP", "CTO", "INVALID",
                "junior!", "mid-level", "sr", "jnr", "123"
        );
    }

    @Provide
    Arbitrary<String> validRemoteStrings() {
        Arbitrary<String> values = Arbitraries.of("REMOTE", "HYBRID", "ON_SITE", "ANY");
        Arbitrary<String> caseVariants = values.flatMap(v ->
                Arbitraries.of(v.toLowerCase(), v.toUpperCase(), capitalize(v), " " + v + " "));
        return caseVariants;
    }

    @Provide
    Arbitrary<String> invalidRemoteStrings() {
        return Arbitraries.of(
                "OFFICE", "HOME", "ONSITE", "FLEXIBLE", "DISTRIBUTED",
                "in-office", "work-from-home", "mixed", "partial", "123"
        );
    }

    @Provide
    Arbitrary<String> nullOrBlankStrings() {
        return Arbitraries.of(null, "", "   ", "\t", "\n");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
