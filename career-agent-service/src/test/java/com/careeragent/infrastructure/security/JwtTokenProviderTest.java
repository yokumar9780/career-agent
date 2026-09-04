package com.careeragent.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider — Requirements 12.2, 12.3")
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-unit-tests-must-be-at-least-64-characters-long-for-hmac-sha512";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateToken returns a non-null, non-empty token")
    void generateToken_returnsNonNullToken() {
        UUID candidateId = UUID.randomUUID();
        String token = tokenProvider.generateToken(candidateId, "user@example.com");

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("generateToken embeds the correct candidateId and email claims")
    void generateToken_containsCorrectClaims() {
        UUID candidateId = UUID.randomUUID();
        String email = "claims@example.com";

        String token = tokenProvider.generateToken(candidateId, email);

        assertThat(tokenProvider.getCandidateId(token)).isEqualTo(candidateId);
        assertThat(tokenProvider.getEmail(token)).isEqualTo(email);
    }

    @Test
    @DisplayName("validateToken returns true for a freshly generated token")
    void validateToken_returnsTrueForValidToken() {
        String token = tokenProvider.generateToken(UUID.randomUUID(), "valid@example.com");

        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for an expired token")
    void validateToken_returnsFalseForExpiredToken() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L); // 1ms expiry
        String token = shortLived.generateToken(UUID.randomUUID(), "expired@example.com");

        Thread.sleep(50); // ensure token has expired

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a malformed token string")
    void validateToken_returnsFalseForMalformedToken() {
        assertThat(tokenProvider.validateToken("not.a.valid.jwt.token")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for null input")
    void validateToken_returnsFalseForNullToken() {
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for an empty string")
    void validateToken_returnsFalseForEmptyToken() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false when a token signed with a different secret")
    void validateToken_returnsFalseForTamperedToken() {
        // Generate a token with a completely different secret key
        String differentSecret = "a-completely-different-secret-key-that-is-at-least-64-characters-long-for-hmac-sha512";
        JwtTokenProvider otherProvider = new JwtTokenProvider(differentSecret, EXPIRATION_MS);
        String tokenFromOtherKey = otherProvider.generateToken(UUID.randomUUID(), "tamper@example.com");

        assertThat(tokenProvider.validateToken(tokenFromOtherKey)).isFalse();
    }

    @Test
    @DisplayName("getCandidateId extracts the correct UUID from a token")
    void getCandidateId_returnsCorrectId() {
        UUID candidateId = UUID.randomUUID();
        String token = tokenProvider.generateToken(candidateId, "id@example.com");

        assertThat(tokenProvider.getCandidateId(token)).isEqualTo(candidateId);
    }

    @Test
    @DisplayName("getEmail extracts the correct email from a token")
    void getEmail_returnsCorrectEmail() {
        String email = "extract@example.com";
        String token = tokenProvider.generateToken(UUID.randomUUID(), email);

        assertThat(tokenProvider.getEmail(token)).isEqualTo(email);
    }
}
