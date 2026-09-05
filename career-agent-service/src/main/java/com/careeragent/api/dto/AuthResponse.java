package com.careeragent.api.dto;

/**
 * Response DTO containing the JWT access token and expiration.
 */
public record AuthResponse(
        String accessToken,
        long expiresIn
) {
}
