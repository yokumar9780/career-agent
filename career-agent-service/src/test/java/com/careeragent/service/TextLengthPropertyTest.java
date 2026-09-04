package com.careeragent.service;

import com.careeragent.api.dto.UpdatePreferenceRequest;
import com.careeragent.api.dto.UpdateProfileRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import net.jqwik.api.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **Validates: Requirements 13.5**
 */
@Label("Feature: career-agent, Property 22: Text Field Length Enforcement")
class TextLengthPropertyTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // --- Profile Name (max 200) ---

    @Property(tries = 200)
    @Label("Profile name within 200 chars → no violations")
    void profileNameWithinLimitIsAccepted(@ForAll("stringsUpTo200") String name) {
        var request = new UpdateProfileRequest(name, null, null);
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Property(tries = 200)
    @Label("Profile name exceeding 200 chars → violation present")
    void profileNameExceedingLimitIsRejected(@ForAll("stringsOver200") String name) {
        var request = new UpdateProfileRequest(name, null, null);
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Example
    @Label("Profile name at exactly 200 chars → accepted")
    void profileNameAtBoundaryIsAccepted() {
        String name = "a".repeat(200);
        var request = new UpdateProfileRequest(name, null, null);
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // --- Summary (max 5000) ---

    @Property(tries = 200)
    @Label("Summary within 5000 chars → no violations")
    void summaryWithinLimitIsAccepted(@ForAll("stringsUpTo5000") String summary) {
        var request = new UpdateProfileRequest(null, null, summary);
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Property(tries = 200)
    @Label("Summary exceeding 5000 chars → violation present")
    void summaryExceedingLimitIsRejected(@ForAll("stringsOver5000") String summary) {
        var request = new UpdateProfileRequest(null, null, summary);
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("summary"));
    }

    // --- Location String (max 200 in preference) ---

    @Property(tries = 200)
    @Label("Location within 200 chars → no violations")
    void locationWithinLimitIsAccepted(@ForAll("stringsUpTo200") String location) {
        var request = new UpdatePreferenceRequest(
                null, List.of(location), null, null, null, null, null, null, null);
        Set<ConstraintViolation<UpdatePreferenceRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Property(tries = 200)
    @Label("Location exceeding 200 chars → violation present")
    void locationExceedingLimitIsRejected(@ForAll("stringsOver200") String location) {
        var request = new UpdatePreferenceRequest(
                null, List.of(location), null, null, null, null, null, null, null);
        Set<ConstraintViolation<UpdatePreferenceRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> stringsUpTo200() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(200);
    }

    @Provide
    Arbitrary<String> stringsOver200() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(201)
                .ofMaxLength(300);
    }

    @Provide
    Arbitrary<String> stringsUpTo5000() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(5000);
    }

    @Provide
    Arbitrary<String> stringsOver5000() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5001)
                .ofMaxLength(6000);
    }
}
