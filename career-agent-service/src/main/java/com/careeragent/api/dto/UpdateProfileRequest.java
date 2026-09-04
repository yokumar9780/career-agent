package com.careeragent.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 200, message = "Name must be at most 200 characters")
        String name,

        @Size(max = 50, message = "Phone must be at most 50 characters")
        String phone,

        @Size(max = 5000, message = "Summary must be at most 5000 characters")
        String summary
) {
}
