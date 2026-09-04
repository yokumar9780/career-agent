package com.careeragent.api.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn
) {
}
