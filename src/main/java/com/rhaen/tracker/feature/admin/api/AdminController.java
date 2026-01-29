package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.admin.api.dto.AdminDtos;
import com.rhaen.tracker.feature.tracking.history.SessionHistoryService;
import com.rhaen.tracker.feature.tracking.persistence.SessionSummaryRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final LastLocationCache lastLocationCache;
    private final TrackingSessionRepository sessionRepository;
    private final SessionSummaryRepository sessionSummaryRepository;
    private final SessionHistoryService sessionHistoryService;

    @GetMapping("/users")
    public ApiResponse<?> users() {
        var items = userRepository.findAll().stream()
                .map(u -> new AdminDtos.UserRow(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.getCreatedAt()
                ))
                .toList();
        return ApiResponse.ok(items);
    }

    /**
     * Dashboard use-case:
     * - show realtime or last known locations for all users
     * For MVP we return each user's ACTIVE session (if any) with lastPoint.
     */
    @GetMapping("/users/last-locations")
    public ApiResponse<?> lastLocations() {
        var items = lastLocationCache.getAll().stream()
                .map(this::lastLocationRow)
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
    public ApiResponse<?> sessionPoints(@PathVariable UUID sessionId,
                                        @RequestParam(required = false) Instant from,
                                        @RequestParam(required = false) Instant to,
                                        @RequestParam(required = false) Integer max) {
        return ApiResponse.ok(sessionHistoryService.getSessionPoints(sessionId, from, to, max));
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ApiResponse<?> sessionSummary(@PathVariable UUID sessionId) {
        var summary = sessionSummaryRepository.findById(sessionId)
                .orElseThrow(() -> new com.rhaen.tracker.common.exception.NotFoundException("Summary not found for session: " + sessionId));
        var bbox = new java.util.LinkedHashMap<String, Object>();
        bbox.put("minLat", summary.getBboxMinLat());
        bbox.put("minLon", summary.getBboxMinLon());
        bbox.put("maxLat", summary.getBboxMaxLat());
        bbox.put("maxLon", summary.getBboxMaxLon());

        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("sessionId", sessionId);
        payload.put("polyline", summary.getPolyline());
        payload.put("simplifiedPolyline", summary.getSimplifiedPolyline());
        payload.put("distanceM", summary.getDistanceM());
        payload.put("durationS", summary.getDurationS());
        payload.put("avgSpeedMps", summary.getAvgSpeedMps());
        payload.put("maxSpeedMps", summary.getMaxSpeedMps());
        payload.put("pointsCount", summary.getPointsCount());
        payload.put("bbox", bbox);
        return ApiResponse.ok(payload);
    }

    @GetMapping("/sessions")
    public ApiResponse<?> sessions(@RequestParam(required = false) UUID userId,
                                   @RequestParam(required = false) TrackingSessionEntity.Status status,
                                   @RequestParam(required = false) Instant from,
                                   @RequestParam(required = false) Instant to,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        var p = sessionRepository.search(userId, status, from, to, pageable);

        var items = p.getContent().stream()
                .map(s -> new AdminDtos.SessionRow(
                        s.getId(),
                        s.getUser().getId(),
                        s.getUser().getUsername(),
                        s.getStartTime(),
                        s.getStopTime(),
                        s.getStatus().name(),
                        s.getLastPointAt()
                ))
                .toList();

        return ApiResponse.ok(java.util.Map.of(
                "items", items,
                "page", p.getNumber(),
                "size", p.getSize(),
                "totalElements", p.getTotalElements(),
                "totalPages", p.getTotalPages()
        ));
    }

    private java.util.Map<String, Object> lastLocationRow(com.rhaen.tracker.feature.tracking.realtime.LastLocationSnapshot snap) {
        var row = new java.util.LinkedHashMap<String, Object>();
        row.put("userId", snap.userId());
        row.put("sessionId", snap.sessionId());
        row.put("status", snap.status());
        row.put("active", snap.active());
        row.put("stale", lastLocationCache.isStale(snap));
        row.put("ts", snap.ts());
        row.put("lat", snap.lat());
        row.put("lon", snap.lon());
        row.put("accuracyM", snap.accuracyM());
        row.put("speedMps", snap.speedMps());
        return row;
    }
}
