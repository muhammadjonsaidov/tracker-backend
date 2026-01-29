package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TrackingSessionRepository sessionRepository;
    private final TrackingPointRepository pointRepository;

    @GetMapping("/users")
    public ApiResponse<?> users() {
        return ApiResponse.ok(userRepository.findAll());
    }

    /**
     * Dashboard use-case:
     * - show realtime or last known locations for all users
     * For MVP we return each user's ACTIVE session (if any) with lastPoint.
     */
    @GetMapping("/users/last-locations")
    public ApiResponse<?> lastLocations() {
        return ApiResponse.ok(sessionRepository.findAll().stream()
                .filter(s -> s.getLastPoint() != null)
                .map(s -> java.util.Map.of(
                        "userId", s.getUser().getId(),
                        "sessionId", s.getId(),
                        "status", s.getStatus().name(),
                        "lastPointAt", s.getLastPointAt(),
                        "lon", s.getLastPoint().getX(),
                        "lat", s.getLastPoint().getY()
                ))
                .toList());
    }

    @GetMapping("/sessions/{sessionId}/points")
    public ApiResponse<?> sessionPoints(@PathVariable UUID sessionId) {
        return ApiResponse.ok(pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId));
    }
}
