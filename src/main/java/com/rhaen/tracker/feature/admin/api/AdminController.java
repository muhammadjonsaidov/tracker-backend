package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.admin.dto.AdminDtos;
import com.rhaen.tracker.feature.admin.query.AdminQueryService;
import com.rhaen.tracker.feature.admin.command.AdminCommandService;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminQueryService adminQueryService;
    private final AdminCommandService adminCommandService;

    @GetMapping("/users")
    @Operation(summary = "Users list", description = "List users with pagination.")
    public ApiResponse<AdminDtos.UserPage> users(
            @Parameter(description = "Page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminQueryService.listUsers(page, size));
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Update user role", description = "Change a user's role (USER/ADMIN).")
    public ApiResponse<Void> updateUserRole(
            @PathVariable UUID userId,
            @RequestParam String role,
            @Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt principal) {
        UUID adminId = UUID.fromString(principal.getSubject());
        adminCommandService.updateUserRole(adminId, userId, role);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Permanently remove a user and their data.")
    public ApiResponse<Void> deleteUser(
            @PathVariable UUID userId,
            @Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt principal) {
        UUID adminId = UUID.fromString(principal.getSubject());
        adminCommandService.deleteUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/users/last-locations")
    @Operation(summary = "Last locations", description = "Returns last known locations for all users.")
    public ApiResponse<List<AdminDtos.LastLocationRow>> lastLocations() {
        return ApiResponse.ok(adminQueryService.listLastLocations());
    }

    @GetMapping("/sessions/{sessionId}/points")
    @Operation(summary = "Session points", description = "Returns raw points with optional window + downsample.")
    public ApiResponse<List<TrackingDtos.PointRow>> sessionPoints(
            @PathVariable UUID sessionId,
            @Parameter(description = "From timestamp (UTC)", example = "2026-01-29T10:00:00Z") @RequestParam(required = false) Instant from,
            @Parameter(description = "To timestamp (UTC)", example = "2026-01-29T11:00:00Z") @RequestParam(required = false) Instant to,
            @Parameter(description = "Max points to return", example = "2000") @RequestParam(required = false) Integer max) {
        return ApiResponse.ok(adminQueryService.getSessionPoints(sessionId, from, to, max));
    }

    @GetMapping("/sessions/{sessionId}/summary")
    @Operation(summary = "Session summary", description = "Returns precomputed session summary.")
    public ApiResponse<AdminDtos.SessionSummaryResponse> sessionSummary(@PathVariable UUID sessionId) {
        return ApiResponse.ok(adminQueryService.getSessionSummary(sessionId));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Sessions list", description = "Search sessions with filters + pagination.")
    public ApiResponse<AdminDtos.SessionPage> sessions(
            @Parameter(description = "Filter by user id") @RequestParam(required = false) UUID userId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) TrackingSessionEntity.Status status,
            @Parameter(description = "From timestamp (UTC)") @RequestParam(required = false) Instant from,
            @Parameter(description = "To timestamp (UTC)") @RequestParam(required = false) Instant to,
            @Parameter(description = "Page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminQueryService.listSessions(userId, status, from, to, page, size));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Audit logs", description = "Returns audit log events with pagination.")
    public ApiResponse<Page<AdminDtos.AuditLogRow>> auditLogs(
            @Parameter(description = "Page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminQueryService.listAuditLogs(page, size));
    }
}
