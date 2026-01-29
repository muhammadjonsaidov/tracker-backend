package com.rhaen.tracker.feature.auth.api.dto;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        authService.register(req);
        return ApiResponse.ok("REGISTERED", null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }
}
