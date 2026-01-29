package com.rhaen.tracker.feature.tracking.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.tracking.command.TrackingCommandService;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.query.TrackingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/sessions")
    @Operation(summary = "My sessions", description = "List sessions for the authenticated user.")
    public ApiResponse<java.util.List<TrackingDtos.SessionRow>> mySessions(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        return ApiResponse.ok(trackingQueryService.listSessions(userId));
    }
}
