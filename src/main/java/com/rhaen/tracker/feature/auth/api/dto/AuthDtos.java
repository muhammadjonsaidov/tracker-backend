package com.rhaen.tracker.feature.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String username,
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record LoginRequest(
            @NotBlank String usernameOrEmail,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String accessToken,
            String tokenType
    ) {}
}
