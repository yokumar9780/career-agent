package com.careeragent.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for candidate login with email and password.
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
