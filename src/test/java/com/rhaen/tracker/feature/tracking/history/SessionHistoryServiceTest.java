package com.rhaen.tracker.feature.tracking.history;

import com.rhaen.tracker.common.exception.BadRequestException;
import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.common.util.GeoUtils;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionHistoryServiceTest {

    @Mock
    private TrackingSessionRepository sessionRepository;
    @Mock
    private TrackingPointRepository pointRepository;

    private SessionHistoryService service;

    @BeforeEach
    void setUp() {
        service = new SessionHistoryService(
                sessionRepository,
                pointRepository,
                new TrackingHistoryProperties(3, 10)
        );
    }

    @Test
    void getSessionPoints_throws_whenSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSessionPoints(sessionId, null, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void getSessionPoints_throws_whenAboveHardLimit() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, UUID.randomUUID())));
        when(pointRepository.countBySessionId(sessionId)).thenReturn(11L);

        assertThatThrownBy(() -> service.getSessionPoints(sessionId, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Too many points");
    }

    @Test
    void getSessionPoints_downsamples_and_keepsLast() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, UUID.randomUUID())));

        List<TrackingPointEntity> points = List.of(
                point(1, 41.0, 69.0),
                point(2, 41.1, 69.1),
                point(3, 41.2, 69.2),
                point(4, 41.3, 69.3),
                point(5, 41.4, 69.4)
        );

        when(pointRepository.countBySessionId(sessionId)).thenReturn(5L);
        when(pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId)).thenReturn(points);

        List<TrackingDtos.PointRow> result = service.getSessionPoints(sessionId, null, null, 3);

        assertThat(result).hasSize(3);
        assertThat(result.getLast().lat()).isEqualTo(41.4);
        assertThat(result.getLast().lon()).isEqualTo(69.4);
    }

    @Test
    void getSessionPointsForUser_throws_whenNonOwnerAndNotAdmin() {
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, ownerId)));

        assertThatThrownBy(() -> service.getSessionPointsForUser(sessionId, otherUser, false, null, null, 10, false, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getSessionPointsForUser_returnsEmpty_whenNoPoints() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, userId)));
        when(pointRepository.countBySessionId(sessionId)).thenReturn(0L);
        when(pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId)).thenReturn(List.of());

        TrackingDtos.PointsResponse result = service.getSessionPointsForUser(sessionId, userId, false, null, null, 5, true, 5.0);

        assertThat(result.points()).isEmpty();
        assertThat(result.truncated()).isFalse();
        assertThat(result.total()).isZero();
    }

    @Test
    void getSessionPointsForUser_nonDownsample_truncatesToMax() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, userId)));

        List<TrackingPointEntity> points = List.of(
                point(1, 41.0, 69.0),
                point(2, 41.1, 69.1),
                point(3, 41.2, 69.2),
                point(4, 41.3, 69.3)
        );
        when(pointRepository.countBySessionId(sessionId)).thenReturn(4L);
        when(pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId)).thenReturn(points);

        TrackingDtos.PointsResponse result = service.getSessionPointsForUser(sessionId, userId, false, null, null, 2, false, 0);

        assertThat(result.points()).hasSize(2);
        assertThat(result.truncated()).isTrue();
        assertThat(result.total()).isEqualTo(4);
    }

    @Test
    void getSessionPointsForUser_rangeBranches_work() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, userId)));

        when(pointRepository.countBySessionIdAndDeviceTimestampBetween(sessionId, from, to)).thenReturn(1L);
        when(pointRepository.findBySessionIdAndDeviceTimestampBetweenOrderByDeviceTimestampAsc(sessionId, from, to))
                .thenReturn(List.of(point(1, 41.0, 69.0)));

        TrackingDtos.PointsResponse between = service.getSessionPointsForUser(sessionId, userId, false, from, to, 10, true, 0);
        assertThat(between.points()).hasSize(1);

        when(pointRepository.countBySessionIdAndDeviceTimestampGreaterThanEqual(sessionId, from)).thenReturn(1L);
        when(pointRepository.findBySessionIdAndDeviceTimestampGreaterThanEqualOrderByDeviceTimestampAsc(sessionId, from))
                .thenReturn(List.of(point(2, 41.2, 69.2)));
        TrackingDtos.PointsResponse fromOnly = service.getSessionPointsForUser(sessionId, userId, false, from, null, 10, true, 0);
        assertThat(fromOnly.points()).hasSize(1);

        when(pointRepository.countBySessionIdAndDeviceTimestampLessThanEqual(sessionId, to)).thenReturn(1L);
        when(pointRepository.findBySessionIdAndDeviceTimestampLessThanEqualOrderByDeviceTimestampAsc(sessionId, to))
                .thenReturn(List.of(point(3, 41.3, 69.3)));
        TrackingDtos.PointsResponse toOnly = service.getSessionPointsForUser(sessionId, userId, false, null, to, 10, true, 0);
        assertThat(toOnly.points()).hasSize(1);

        verify(pointRepository).countBySessionIdAndDeviceTimestampBetween(sessionId, from, to);
        verify(pointRepository).countBySessionIdAndDeviceTimestampGreaterThanEqual(sessionId, from);
        verify(pointRepository).countBySessionIdAndDeviceTimestampLessThanEqual(sessionId, to);
    }

    @Test
    void getSessionPointsForUser_rdpSimplifies_whenEnabled() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, userId)));

        List<TrackingPointEntity> points = List.of(
                point(1, 41.0000, 69.0000),
                point(2, 41.0004, 69.0000),
                point(3, 41.0008, 69.0000),
                point(4, 41.0012, 69.0000),
                point(5, 41.0016, 69.0000)
        );
        when(pointRepository.countBySessionId(sessionId)).thenReturn((long) points.size());
        when(pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId)).thenReturn(points);

        TrackingDtos.PointsResponse result = service.getSessionPointsForUser(sessionId, userId, false, null, null, 10, true, 50.0);

        assertThat(result.points().size()).isLessThanOrEqualTo(points.size());
        assertThat(result.total()).isEqualTo(points.size());
    }

    private static TrackingSessionEntity session(UUID sessionId, UUID userId) {
        return TrackingSessionEntity.builder()
                .id(sessionId)
                .user(UserEntity.builder().id(userId).username("u").email("e@e.com").passwordHash("x").role(UserEntity.Role.USER).build())
                .status(TrackingSessionEntity.Status.ACTIVE)
                .startTime(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private static TrackingPointEntity point(int second, double lat, double lon) {
        return TrackingPointEntity.builder()
                .eventId(UUID.randomUUID())
                .deviceTimestamp(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(second))
                .receivedAt(Instant.now())
                .point(GeoUtils.point(lon, lat))
                .build();
    }
}
