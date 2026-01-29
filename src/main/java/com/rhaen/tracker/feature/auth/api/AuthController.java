package com.rhaen.tracker.feature.auth.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.auth.command.AuthCommandService;
import com.rhaen.tracker.feature.auth.dto.AuthDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthCommandService authService;

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Creates a new user with USER role.")
    public ApiResponse<Void> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        authService.register(req);
        return ApiResponse.ok("REGISTERED", null);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Returns JWT access token.")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }
}
