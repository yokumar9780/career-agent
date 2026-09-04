package com.careeragent.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String email,
        String name,
        String phone,
        String summary,
        String applicationMode,
        String preSubmitReview,
        int matchScoreThreshold,
        String timezone,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
