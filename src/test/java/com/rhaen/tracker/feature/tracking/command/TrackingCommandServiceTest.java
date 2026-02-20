package com.rhaen.tracker.feature.tracking.command;

import com.rhaen.tracker.common.audit.AuditService;
import com.rhaen.tracker.common.exception.ConflictException;
import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.common.exception.TooManyRequestsException;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.ingest.RedisRateLimiter;
import com.rhaen.tracker.feature.tracking.ingest.TrackingIngestProperties;
import com.rhaen.tracker.feature.tracking.ingest.TrackingPointIngestRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.feature.tracking.summary.SessionSummaryService;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingCommandServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private TrackingSessionRepository sessionRepository;
    @Mock
    private LastLocationCache lastLocationCache;
    @Mock
    private SessionSummaryService sessionSummaryService;
    @Mock
    private TrackingIngestProperties ingestProps;
    @Mock
    private RedisRateLimiter rateLimiter;
    @Mock
    private TrackingPointIngestRepository ingestRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private Counter sessionStartCounter;
    @Mock
    private Counter sessionStopCounter;
    @Mock
    private Counter ingestRequestsCounter;
    @Mock
    private Counter pointsAcceptedCounter;
    @Mock
    private Counter pointsInsertedCounter;
    @Mock
    private Timer ingestTimer;

    private TrackingCommandService service;
    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        service = new TrackingCommandService(
                userRepository,
                sessionRepository,
                lastLocationCache,
                sessionSummaryService,
                ingestProps,
                rateLimiter,
                ingestRepository,
                auditService,
                meterRegistry);
        ReflectionTestUtils.setField(service, "sessionStartCounter", sessionStartCounter);
        ReflectionTestUtils.setField(service, "sessionStopCounter", sessionStopCounter);
        ReflectionTestUtils.setField(service, "ingestRequestsCounter", ingestRequestsCounter);
        ReflectionTestUtils.setField(service, "pointsAcceptedCounter", pointsAcceptedCounter);
        ReflectionTestUtils.setField(service, "pointsInsertedCounter", pointsInsertedCounter);
        ReflectionTestUtils.setField(service, "ingestTimer", ingestTimer);
    }

    @Test
    void startSession_success() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).username("u").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRepository.findActiveByUserId(userId)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(TrackingSessionEntity.class))).thenAnswer(inv -> {
            TrackingSessionEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        var resp = service.startSession(userId);

        assertThat(resp.sessionId()).isNotNull();
        assertThat(resp.status()).isEqualTo("ACTIVE");
        verify(sessionStartCounter).increment();
    }

    @Test
    void startSession_whenUserHasActive_throws() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        TrackingSessionEntity active = TrackingSessionEntity.builder().id(UUID.randomUUID()).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRepository.findActiveByUserId(userId)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.startSession(userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has ACTIVE");
    }

    @Test
    void stopSession_buildsSummaryAndStops() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        var point = gf.createPoint(new Coordinate(69.2, 41.3));
        TrackingSessionEntity session = TrackingSessionEntity.builder()
                .id(sessionId)
                .user(user)
                .status(TrackingSessionEntity.Status.ACTIVE)
                .lastPoint(point)
                .build();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        service.stopSession(sessionId, userId, new TrackingDtos.StopSessionRequest(null, null, null));

        assertThat(session.getStatus()).isEqualTo(TrackingSessionEntity.Status.STOPPED);
        verify(sessionSummaryService).buildOrRebuild(session);
        verify(sessionStopCounter).increment();
    }

    @Test
    void stopSession_whenMissing_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.stopSession(sessionId, userId, new TrackingDtos.StopSessionRequest(null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void ingestPoints_whenRateLimited_throws429() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        TrackingSessionEntity session = TrackingSessionEntity.builder()
                .id(sessionId)
                .user(user)
                .status(TrackingSessionEntity.Status.ACTIVE)
                .build();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(ingestProps.maxBatchSize()).thenReturn(100);
        when(rateLimiter.consumePoints(userId, 1)).thenReturn(new RedisRateLimiter.RateLimitResult(false, 999));

        var req = new TrackingDtos.IngestPointsRequest(List.of(
                new TrackingDtos.LocationPoint(
                        UUID.randomUUID(),
                        41.31,
                        69.27,
                        Instant.now(),
                        1f,
                        1f,
                        20f,
                        "gps",
                        false)));

        assertThatThrownBy(() -> service.ingestPoints(sessionId, userId, req))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
