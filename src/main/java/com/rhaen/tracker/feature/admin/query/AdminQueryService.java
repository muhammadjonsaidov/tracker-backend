package com.rhaen.tracker.feature.admin.query;

import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.feature.admin.dto.AdminDtos;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.history.SessionHistoryService;
import com.rhaen.tracker.feature.tracking.persistence.SessionSummaryRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminQueryService {

    private final UserRepository userRepository;
    private final TrackingSessionRepository sessionRepository;
    private final SessionSummaryRepository sessionSummaryRepository;
    private final SessionHistoryService sessionHistoryService;
    private final LastLocationCache lastLocationCache;

    public List<AdminDtos.UserRow> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new AdminDtos.UserRow(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.getCreatedAt()
                ))
                .toList();
    }

    public List<AdminDtos.LastLocationRow> listLastLocations() {
        return lastLocationCache.getAll().stream()
                .map(snap -> new AdminDtos.LastLocationRow(
                        snap.userId(),
                        snap.sessionId(),
                        snap.status(),
                        snap.active(),
                        lastLocationCache.isStale(snap),
                        snap.ts(),
                        snap.lat(),
                        snap.lon(),
                        snap.accuracyM(),
                        snap.speedMps(),
                        snap.headingDeg()
                ))
                .sorted((a, b) -> {
                    if (a.active() != b.active()) {
                        return a.active() ? -1 : 1;
                    }
                    Instant ta = a.ts();
                    Instant tb = b.ts();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                })
                .toList();
    }

    public AdminDtos.SessionPage listSessions(UUID userId,
                                              TrackingSessionEntity.Status status,
                                              Instant from,
                                              Instant to,
                                              int page,
                                              int size) {
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

        return new AdminDtos.SessionPage(
                items,
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages()
        );
    }

    public AdminDtos.SessionSummaryResponse getSessionSummary(UUID sessionId) {
        var summary = sessionSummaryRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Summary not found for session: " + sessionId));
        var bbox = new AdminDtos.Bbox(
                summary.getBboxMinLat(),
                summary.getBboxMinLon(),
                summary.getBboxMaxLat(),
                summary.getBboxMaxLon()
        );
        return new AdminDtos.SessionSummaryResponse(
                sessionId,
                summary.getPolyline(),
                summary.getSimplifiedPolyline(),
                summary.getDistanceM(),
                summary.getDurationS(),
                summary.getAvgSpeedMps(),
                summary.getMaxSpeedMps(),
                summary.getPointsCount(),
                bbox,
                summary.getRawPointsPrunedAt()
        );
    }

    public List<TrackingDtos.PointRow> getSessionPoints(UUID sessionId,
                                                       Instant from,
                                                       Instant to,
                                                       Integer max) {
        return sessionHistoryService.getSessionPoints(sessionId, from, to, max);
    }
}
