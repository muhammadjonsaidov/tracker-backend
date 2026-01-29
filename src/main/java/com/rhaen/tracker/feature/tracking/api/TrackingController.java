package com.rhaen.tracker.feature.tracking.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.tracking.api.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/sessions/start")
    public ApiResponse<TrackingDtos.StartSessionResponse> start(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        return ApiResponse.ok(trackingService.startSession(userId));
    }

    @PostMapping("/sessions/{sessionId}/stop")
    public ApiResponse<Void> stop(@PathVariable UUID sessionId,
                                  @AuthenticationPrincipal Jwt jwt,
                                  @RequestBody TrackingDtos.StopSessionRequest req) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        trackingService.stopSession(sessionId, userId, req);
        return ApiResponse.ok("STOPPED", null);
    }

    @PostMapping("/sessions/{sessionId}/points")
    public ApiResponse<Object> ingest(@PathVariable UUID sessionId,
                                      @AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody TrackingDtos.IngestPointsRequest req) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        int inserted = trackingService.ingestPoints(sessionId, userId, req);
        int accepted = req.points().size();
        return ApiResponse.ok("ACCEPTED", java.util.Map.of(
                "accepted", accepted,
                "inserted", inserted
        ));
    }

    @GetMapping("/sessions")
    public ApiResponse<?> mySessions(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("uid"));
        return ApiResponse.ok(trackingService.listSessions(userId));
    }
}
