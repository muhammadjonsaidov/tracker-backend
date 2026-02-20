package com.rhaen.tracker.feature.tracking.command;

import com.rhaen.tracker.common.audit.AuditService;
import com.rhaen.tracker.common.exception.BadRequestException;
import com.rhaen.tracker.common.exception.ConflictException;
import com.rhaen.tracker.common.exception.ForbiddenException;
import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.common.exception.TooManyRequestsException;
import com.rhaen.tracker.common.util.GeoUtils;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.ingest.RedisRateLimiter;
import com.rhaen.tracker.feature.tracking.ingest.TrackingIngestProperties;
import com.rhaen.tracker.feature.tracking.ingest.TrackingPointIngestRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationSnapshot;
import com.rhaen.tracker.feature.tracking.summary.SessionSummaryService;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackingCommandService {

    private final UserRepository userRepository;
    private final TrackingSessionRepository sessionRepository;

    private final LastLocationCache lastLocationCache;
    private final SessionSummaryService sessionSummaryService;

    // Step 8 deps
    private final TrackingIngestProperties ingestProps;
    private final RedisRateLimiter rateLimiter;
    private final TrackingPointIngestRepository ingestRepository;
    private final AuditService auditService;

    private final MeterRegistry meterRegistry;

    private Counter sessionStartCounter;
    private Counter sessionStopCounter;

    private Counter ingestRequestsCounter;
    private Counter pointsAcceptedCounter;
    private Counter pointsInsertedCounter;

    private Timer ingestTimer;

    @Transactional
    public TrackingDtos.StartSessionResponse startSession(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        sessionRepository.findActiveByUserId(userId).ifPresent(s -> {
            throw new ConflictException("User already has ACTIVE session: " + s.getId());
        });

        TrackingSessionEntity session = TrackingSessionEntity.builder()
                .user(user)
                .startTime(Instant.now())
                .status(TrackingSessionEntity.Status.ACTIVE)
                .updatedAt(Instant.now())
                .build();

        session = sessionRepository.save(session);

        sessionStartCounter.increment();
        auditService.logUserAction(
                userId,
                "SESSION_START",
                "SESSION",
                session.getId(),
                Map.of("status", session.getStatus().name()));

        return new TrackingDtos.StartSessionResponse(
                session.getId(),
                session.getStartTime(),
                session.getStatus().name());
    }

    @Transactional
    public void stopSession(UUID sessionId, UUID userId, TrackingDtos.StopSessionRequest req) {
        TrackingSessionEntity session = requireOwnedSession(sessionId, userId);

        if (session.getStatus() != TrackingSessionEntity.Status.ACTIVE) {
            throw new BadRequestException("Session is not ACTIVE: " + session.getStatus());
        }

        Instant stopTime = (req.stopTime() != null) ? req.stopTime() : Instant.now();
        session.setStopTime(stopTime);

        // Stop point priority:
        // 1) request coords
        // 2) lastPoint
        if (req.stopLat() != null && req.stopLon() != null) {
            session.setStopPoint(GeoUtils.point(req.stopLon(), req.stopLat()));
        } else if (session.getStopPoint() == null && session.getLastPoint() != null) {
            session.setStopPoint(session.getLastPoint());
        }

        session.setStatus(TrackingSessionEntity.Status.STOPPED);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        // Summary rebuild AFTER session has final stop_time/status/stop_point
        sessionSummaryService.buildOrRebuild(session);

        // Update Redis last_location (active=false)
        var p = (session.getStopPoint() != null) ? session.getStopPoint() : session.getLastPoint();
        if (p != null) {
            lastLocationCache.upsert(new LastLocationSnapshot(
                    session.getUser().getId(),
                    session.getId(),
                    session.getStatus().name(),
                    false,
                    p.getY(),
                    p.getX(),
                    stopTime,
                    null, null, null));
        }

        sessionStopCounter.increment();
        auditService.logUserAction(
                userId,
                "SESSION_STOP",
                "SESSION",
                session.getId(),
                Map.of("status", session.getStatus().name()));
    }

    /**
     * Returns: inserted rows count (DB level).
     * NOTE: accepted points = req.points().size()
     */
    @Transactional
    public int ingestPoints(UUID sessionId, UUID userId, TrackingDtos.IngestPointsRequest req) {
        TrackingSessionEntity session = requireOwnedSession(sessionId, userId);

        if (session.getStatus() != TrackingSessionEntity.Status.ACTIVE) {
            throw new BadRequestException("Session is not ACTIVE: " + session.getStatus());
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        ingestRequestsCounter.increment();

        int rawCount = req.points().size();
        pointsAcceptedCounter.increment(rawCount);

        try {
            // Guard: max batch size
            if (rawCount > ingestProps.maxBatchSize()) {
                throw new BadRequestException("Too many points in one request: " + rawCount
                        + ". Max is " + ingestProps.maxBatchSize());
            }

            // Rate limit: points/minute (fixed window)
            var rl = rateLimiter.consumePoints(userId, rawCount);
            if (!rl.allowed()) {
                auditService.logUserAction(
                        userId,
                        "RATE_LIMIT_VIOLATION",
                        "SESSION",
                        sessionId,
                        Map.of("current", rl.current(), "attemptedPoints", rawCount));
                throw new TooManyRequestsException("Rate limit exceeded (points/min). Current=" + rl.current());
            }

            // Dedupe inside the same batch by eventId (keep first occurrence)
            Map<UUID, TrackingPointIngestRepository.IngestPointRow> unique = req.points().stream()
                    .collect(Collectors.toMap(
                            TrackingDtos.LocationPoint::eventId,
                            p -> new TrackingPointIngestRepository.IngestPointRow(
                                    p.eventId(),
                                    p.lat(),
                                    p.lon(),
                                    p.deviceTimestamp(),
                                    p.accuracyM(),
                                    p.speedMps(),
                                    p.headingDeg(),
                                    p.provider(),
                                    Boolean.TRUE.equals(p.mock())),
                            (first, second) -> first));

            List<TrackingPointIngestRepository.IngestPointRow> rows = unique.values().stream().toList();
            if (rows.isEmpty())
                return 0;

            Instant receivedAt = Instant.now();

            // Insert points with ON CONFLICT DO NOTHING (no crash on duplicates)
            int inserted = ingestRepository.insertBatch(session.getId(), receivedAt, rows);

            // Find earliest + latest by device timestamp
            var earliest = rows.stream()
                    .min(Comparator.comparing(TrackingPointIngestRepository.IngestPointRow::deviceTimestamp))
                    .orElseThrow();

            var latest = rows.stream()
                    .max(Comparator.comparing(TrackingPointIngestRepository.IngestPointRow::deviceTimestamp))
                    .orElseThrow();

            // Update session "last known"
            if (session.getStartPoint() == null) {
                session.setStartPoint(GeoUtils.point(earliest.lon(), earliest.lat()));
            }
            session.setLastPoint(GeoUtils.point(latest.lon(), latest.lat()));
            session.setLastPointAt(latest.deviceTimestamp());
            session.setUpdatedAt(Instant.now());
            sessionRepository.save(session);

            // Update Redis last_location (active=true)
            lastLocationCache.upsert(new LastLocationSnapshot(
                    session.getUser().getId(),
                    session.getId(),
                    session.getStatus().name(),
                    true,
                    latest.lat(),
                    latest.lon(),
                    latest.deviceTimestamp(),
                    latest.accuracyM(),
                    latest.speedMps(),
                    latest.headingDeg()));

            pointsInsertedCounter.increment(inserted);
            return inserted;
        } finally {
            sample.stop(ingestTimer);
        }
    }

    @PostConstruct
    void initMetrics() {
        sessionStartCounter = Counter.builder("tracker.session.start.total").register(meterRegistry);
        sessionStopCounter = Counter.builder("tracker.session.stop.total").register(meterRegistry);

        ingestRequestsCounter = Counter.builder("tracker.ingest.requests.total").register(meterRegistry);
        pointsAcceptedCounter = Counter.builder("tracker.ingest.points.accepted.total").register(meterRegistry);
        pointsInsertedCounter = Counter.builder("tracker.ingest.points.inserted.total").register(meterRegistry);

        ingestTimer = Timer.builder("tracker.ingest.duration")
                .description("Ingest processing duration")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    private TrackingSessionEntity requireOwnedSession(UUID sessionId, UUID userId) {
        TrackingSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            auditService.logUserAction(
                    userId,
                    "AUTHORIZATION_FAILED",
                    "SESSION",
                    sessionId,
                    Map.of("reason", "session_not_owned"));
            throw new ForbiddenException("Session does not belong to user");
        }
        return session;
    }
}
