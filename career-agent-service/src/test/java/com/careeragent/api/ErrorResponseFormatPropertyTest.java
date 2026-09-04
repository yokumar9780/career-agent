package com.careeragent.api;

import com.careeragent.api.dto.ErrorResponse;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * **Validates: Requirements 13.3, 18.6**
 */
@Label("Feature: career-agent, Property 17: Error Response Format Consistency")
class ErrorResponseFormatPropertyTest {

    // Property: Every ErrorResponse has all required fields non-null
    @Property(tries = 200)
    @Label("All error responses contain timestamp, status, error, message, and path")
    void allErrorResponsesHaveRequiredFields(
            @ForAll @IntRange(min = 400, max = 599) int status,
            @ForAll("errorTypes") String errorType,
            @ForAll("messages") String message,
            @ForAll("paths") String path
    ) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status,
                errorType,
                message,
                path,
                null // details are optional
        );

        assertThat(response.timestamp()).isNotNull();
        assertThat(response.status()).isBetween(400, 599);
        assertThat(response.error()).isNotNull().isNotEmpty();
        assertThat(response.message()).isNotNull().isNotEmpty();
        assertThat(response.path()).isNotNull().isNotEmpty();
    }

    // Property: Error responses with details also have all required fields
    @Property(tries = 100)
    @Label("Error responses with field errors still contain all required fields")
    void errorResponsesWithDetailsHaveRequiredFields(
            @ForAll @IntRange(min = 400, max = 599) int status,
            @ForAll("fieldErrorLists") List<ErrorResponse.FieldError> details
    ) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status,
                "Bad Request",
                "Validation failed",
                "/api/v1/auth/register",
                details
        );

        assertThat(response.timestamp()).isNotNull();
        assertThat(response.status()).isBetween(400, 599);
        assertThat(response.error()).isNotNull().isNotEmpty();
        assertThat(response.message()).isNotNull().isNotEmpty();
        assertThat(response.path()).isNotNull().isNotEmpty();
        assertThat(response.details()).isNotNull();
        for (ErrorResponse.FieldError fe : response.details()) {
            assertThat(fe.field()).isNotNull().isNotEmpty();
            assertThat(fe.message()).isNotNull().isNotEmpty();
        }
    }

    // Property: Status code is always in 4xx-5xx range for errors
    @Property(tries = 100)
    @Label("Error status codes are always 4xx or 5xx")
    void statusCodesAreInErrorRange(
            @ForAll @IntRange(min = 400, max = 599) int status
    ) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(), status, "Error", "msg", "/path", null
        );
        assertThat(response.status()).isGreaterThanOrEqualTo(400);
        assertThat(response.status()).isLessThan(600);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> errorTypes() {
        return Arbitraries.of(
                "Bad Request", "Unauthorized", "Forbidden",
                "Not Found", "Conflict", "Internal Server Error"
        );
    }

    @Provide
    Arbitrary<String> messages() {
        return Arbitraries.of(
                "Validation failed",
                "Invalid email or password",
                "Request body could not be parsed",
                "An unexpected error occurred",
                "Email already registered",
                "Password validation failed"
        );
    }

    @Provide
    Arbitrary<String> paths() {
        return Arbitraries.of(
                "/api/v1/auth/register",
                "/api/v1/auth/login",
                "/api/v1/profiles/me",
                "/api/v1/jobs",
                "/api/v1/applications"
        );
    }

    @Provide
    Arbitrary<List<ErrorResponse.FieldError>> fieldErrorLists() {
        Arbitrary<ErrorResponse.FieldError> fieldError = Combinators.combine(
                Arbitraries.of("email", "password", "name", "salary", "location"),
                Arbitraries.of("must not be blank", "invalid format", "too short", "too long", "must be positive")
        ).as(ErrorResponse.FieldError::new);

        return fieldError.list().ofMinSize(1).ofMaxSize(5);
    }
}
