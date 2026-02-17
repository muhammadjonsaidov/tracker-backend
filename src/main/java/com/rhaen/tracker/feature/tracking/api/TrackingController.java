package com.rhaen.tracker.feature.tracking.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.tracking.command.TrackingCommandService;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.query.TrackingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tracking")
@Tag(name = "Tracking", description = "Tracking endpoints for mobile clients")
@SecurityRequirement(name = "bearerAuth")
public class TrackingController {

    private final TrackingCommandService trackingCommandService;
    private final TrackingQueryService trackingQueryService;

    @PostMapping("/sessions/start")
    @Operation(summary = "Start tracking session", description = "Creates a new ACTIVE session for the user.")
    public ApiResponse<TrackingDtos.StartSessionResponse> start(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        return ApiResponse.ok(trackingCommandService.startSession(userId));
    }

    @PostMapping("/sessions/{sessionId}/stop")
    @Operation(summary = "Stop tracking session", description = "Stops an ACTIVE session and finalizes summary.")
    public ApiResponse<Void> stop(@PathVariable UUID sessionId,
                                  @AuthenticationPrincipal Jwt jwt,
                                  @RequestBody TrackingDtos.StopSessionRequest req) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        trackingCommandService.stopSession(sessionId, userId, req);
        return ApiResponse.ok("STOPPED", null);
    }

    @PostMapping("/sessions/{sessionId}/points")
    @Operation(summary = "Ingest tracking points", description = "Accepts a batch of location points.")
    public ApiResponse<TrackingDtos.IngestResponse> ingest(@PathVariable UUID sessionId,
                                      @AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody TrackingDtos.IngestPointsRequest req) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        int inserted = trackingCommandService.ingestPoints(sessionId, userId, req);
        int accepted = req.points().size();
        return ApiResponse.ok(new TrackingDtos.IngestResponse(accepted, inserted));
    }

    @GetMapping("/sessions/{sessionId}/points")
    @Operation(summary = "My session points", description = "Returns raw points with optional window + downsample.")
    public ApiResponse<TrackingDtos.PointsResponse> sessionPoints(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "From timestamp (UTC)", example = "2026-01-29T10:00:00Z")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "To timestamp (UTC)", example = "2026-01-29T11:00:00Z")
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Max points to return", example = "2000")
            @RequestParam(required = false) Integer max,
            @Parameter(description = "Downsample points", example = "true")
            @RequestParam(defaultValue = "true") boolean downsample,
            @Parameter(description = "Simplify epsilon in meters", example = "8")
            @RequestParam(defaultValue = "8") double simplifyEpsM) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        List<String> roles = jwt.getClaimAsStringList("roles");
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        return ApiResponse.ok(trackingQueryService.getSessionPoints(
                sessionId,
                userId,
                isAdmin,
                from,
                to,
                max,
                downsample,
                simplifyEpsM
        ));
    }

    @GetMapping("/sessions")
    @Operation(summary = "My sessions", description = "List sessions for the authenticated user.")
    public ApiResponse<List<TrackingDtos.SessionRow>> mySessions(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        return ApiResponse.ok(trackingQueryService.listSessions(userId));
    }
}
