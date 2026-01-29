package com.rhaen.tracker.feature.tracking.service;

import com.rhaen.tracker.common.exception.BadRequestException;
import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.common.util.GeoUtils;
import com.rhaen.tracker.feature.tracking.api.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.persistence.*;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final UserRepository userRepository;
    private final TrackingSessionRepository sessionRepository;
    private final TrackingPointRepository pointRepository;

    @Transactional
    public TrackingDtos.StartSessionResponse startSession(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        sessionRepository.findActiveByUserId(userId).ifPresent(s -> {
            throw new BadRequestException("User already has ACTIVE session: " + s.getId());
        });

        TrackingSessionEntity session = TrackingSessionEntity.builder()
                .user(user)
                .startTime(Instant.now())
                .status(TrackingSessionEntity.Status.ACTIVE)
                .updatedAt(Instant.now())
                .build();

        session = sessionRepository.save(session);

        return new TrackingDtos.StartSessionResponse(session.getId(), session.getStartTime(), session.getStatus().name());
    }

    @Transactional
    public void stopSession(UUID sessionId, UUID userId, TrackingDtos.StopSessionRequest req) {
        TrackingSessionEntity session = requireOwnedSession(sessionId, userId);

        if (session.getStatus() != TrackingSessionEntity.Status.ACTIVE) {
            throw new BadRequestException("Session is not ACTIVE: " + session.getStatus());
        }

        session.setStopTime(req.stopTime() != null ? req.stopTime() : Instant.now());
        session.setStatus(TrackingSessionEntity.Status.STOPPED);
        if (req.stopLat() != null && req.stopLon() != null) {
            session.setStopPoint(GeoUtils.point(req.stopLon(), req.stopLat()));
        }
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        // Next step (you'll implement): generate session_summary (polyline, distance, duration)
    }

    @Transactional
    public int ingestPoints(UUID sessionId, UUID userId, TrackingDtos.IngestPointsRequest req) {
        TrackingSessionEntity session = requireOwnedSession(sessionId, userId);

        if (session.getStatus() != TrackingSessionEntity.Status.ACTIVE) {
            throw new BadRequestException("Session is not ACTIVE: " + session.getStatus());
        }

        List<TrackingPointEntity> entities = req.points().stream().map(p -> TrackingPointEntity.builder()
                .session(session)
                .eventId(p.eventId() != null ? p.eventId() : UUID.randomUUID())
                .deviceTimestamp(p.deviceTimestamp())
                .receivedAt(Instant.now())
                .point(GeoUtils.point(p.lon(), p.lat()))
                .accuracyM(p.accuracyM())
                .speedMps(p.speedMps())
                .headingDeg(p.headingDeg())
                .provider(p.provider())
                .mock(Boolean.TRUE.equals(p.mock()))
                .build()
        ).toList();

        pointRepository.saveAll(entities);

        // Update session "last known"
        TrackingPointEntity last = entities.getLast();
        if (session.getStartPoint() == null) {
            session.setStartPoint(last.getPoint());
        }
        session.setLastPoint(last.getPoint());
        session.setLastPointAt(last.getDeviceTimestamp());
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        return entities.size();
    }

    private TrackingSessionEntity requireOwnedSession(UUID sessionId, UUID userId) {
        TrackingSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new BadRequestException("Session does not belong to user");
        }
        return session;
    }
}
