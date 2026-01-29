package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointRepository;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TrackingPointRepository pointRepository;
    private final LastLocationCache lastLocationCache;

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
        var items = lastLocationCache.getAll().stream()
                .map(snap -> java.util.Map.of(
                        "userId", snap.userId(),
                        "sessionId", snap.sessionId(),
                        "status", snap.status(),
                        "active", snap.active(),
                        "stale", lastLocationCache.isStale(snap),
                        "ts", snap.ts(),
                        "lat", snap.lat(),
                        "lon", snap.lon(),
                        "accuracyM", snap.accuracyM(),
                        "speedMps", snap.speedMps()
                ))
                // active first, then newest
                .sorted((a, b) -> {
                    boolean aActive = Boolean.TRUE.equals(a.get("active"));
                    boolean bActive = Boolean.TRUE.equals(b.get("active"));
                    if (aActive != bActive) return aActive ? -1 : 1;
                    var ta = (java.time.Instant) a.get("ts");
                    var tb = (java.time.Instant) b.get("ts");
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                })
                .toList();

        return ApiResponse.ok(items);
    }

    @GetMapping("/sessions/{sessionId}/points")
    public ApiResponse<?> sessionPoints(@PathVariable UUID sessionId) {
        return ApiResponse.ok(pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId));
    }
}
