package com.rhaen.tracker.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record RegisterRequest(
            @Schema(description = "Unique username", example = "user1")
            @NotBlank String username,
            @Schema(description = "Valid email address", example = "user1@mail.com")
            @Email @NotBlank String email,
            @Schema(description = "Plain password (min 6+ recommended)", example = "pass12345")
            @NotBlank String password
    ) {}

    public record LoginRequest(
            @Schema(description = "Username or email", example = "user1")
            @NotBlank String usernameOrEmail,
            @Schema(description = "Plain password", example = "pass12345")
            @NotBlank String password
    ) {}

    public record AuthResponse(
            @Schema(description = "JWT access token")
            String accessToken,
            @Schema(description = "Token type", example = "Bearer")
            String tokenType
    ) {}
}
