package com.rhaen.tracker.feature.tracking.expiry;

import com.rhaen.tracker.common.util.GeoUtils;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.feature.tracking.summary.SessionSummaryService;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionExpiryJobTest {

    @Mock
    private TrackingSessionRepository sessionRepository;
    @Mock
    private SessionSummaryService summaryService;
    @Mock
    private LastLocationCache lastLocationCache;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter counter;

    private SessionExpiryJob job;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("tracker.session.expired.total")).thenReturn(counter);
        job = new SessionExpiryJob(
                sessionRepository,
                summaryService,
                lastLocationCache,
                new SessionExpiryProperties(300, 600, 60, 100),
                meterRegistry
        );
        ReflectionTestUtils.invokeMethod(job, "init");
    }

    @Test
    void sweep_expiresSessions_andBuildsSummary_andUpdatesCache() {
        TrackingSessionEntity s1 = activeSession(true, true);
        TrackingSessionEntity s2 = activeSession(false, true);

        when(sessionRepository.findByStatusAndLastPointAtBefore(eq(TrackingSessionEntity.Status.ACTIVE), any(), any()))
                .thenReturn(new PageImpl<>(List.of(s1)));
        when(sessionRepository.findByStatusAndLastPointAtIsNullAndStartTimeBefore(eq(TrackingSessionEntity.Status.ACTIVE), any(), any()))
                .thenReturn(new PageImpl<>(List.of(s2)));

        job.sweep();

        verify(sessionRepository, atLeast(2)).save(any(TrackingSessionEntity.class));
        verify(summaryService, times(2)).buildOrRebuild(any(TrackingSessionEntity.class));
        verify(lastLocationCache, atLeastOnce()).upsert(any());
        verify(counter, times(2)).increment();
    }

    @Test
    void sweep_skips_nonActive_sessions() {
        TrackingSessionEntity stopped = activeSession(true, true);
        stopped.setStatus(TrackingSessionEntity.Status.STOPPED);

        when(sessionRepository.findByStatusAndLastPointAtBefore(eq(TrackingSessionEntity.Status.ACTIVE), any(), any()))
                .thenReturn(new PageImpl<>(List.of(stopped)));
        when(sessionRepository.findByStatusAndLastPointAtIsNullAndStartTimeBefore(eq(TrackingSessionEntity.Status.ACTIVE), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        job.sweep();

        verify(sessionRepository, never()).save(stopped);
        verify(summaryService, never()).buildOrRebuild(any());
        verify(counter, never()).increment();
    }

    private static TrackingSessionEntity activeSession(boolean hasLastPointAt, boolean hasPoint) {
        TrackingSessionEntity session = TrackingSessionEntity.builder()
                .id(UUID.randomUUID())
                .user(UserEntity.builder().id(UUID.randomUUID()).username("u").email("u@e.com").passwordHash("x").role(UserEntity.Role.USER).build())
                .status(TrackingSessionEntity.Status.ACTIVE)
                .startTime(Instant.now().minusSeconds(500))
                .updatedAt(Instant.now())
                .build();
        if (hasLastPointAt) {
            session.setLastPointAt(Instant.now().minusSeconds(400));
        }
        if (hasPoint) {
            session.setStartPoint(GeoUtils.point(69.0, 41.0));
            session.setLastPoint(GeoUtils.point(69.01, 41.01));
        }
        return session;
    }
}
