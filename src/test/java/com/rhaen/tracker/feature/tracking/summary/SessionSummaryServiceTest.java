package com.rhaen.tracker.feature.tracking.summary;

import com.rhaen.tracker.feature.tracking.persistence.SessionSummaryEntity;
import com.rhaen.tracker.feature.tracking.persistence.SessionSummaryRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionSummaryServiceTest {
    @Mock
    private TrackingPointRepository pointRepository;
    @Mock
    private SessionSummaryRepository summaryRepository;
    @Mock
    private TrackingSummaryProperties props;

    private SessionSummaryService service;
    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        service = new SessionSummaryService(pointRepository, summaryRepository, props);
        when(props.maxPolylinePoints()).thenReturn(10000);
        when(props.simplifyEpsilonM()).thenReturn(10.0);
    }

    @Test
    void buildOrRebuild_emptyPoints_setsZeroAndEmptyStrings() {
        UUID id = UUID.randomUUID();
        var session = TrackingSessionEntity.builder()
                .id(id)
                .startTime(Instant.now())
                .stopTime(Instant.now())
                .build();
        when(pointRepository.findBySessionIdOrderByDeviceTimestampAsc(id)).thenReturn(List.of());
        when(summaryRepository.findById(id)).thenReturn(Optional.empty());

        service.buildOrRebuild(session);

        ArgumentCaptor<SessionSummaryEntity> captor = ArgumentCaptor.forClass(SessionSummaryEntity.class);
        verify(summaryRepository).save(captor.capture());
        SessionSummaryEntity s = captor.getValue();
        assertThat(s.getPointsCount()).isEqualTo(0);
        assertThat(s.getDistanceM()).isEqualTo(0d);
        assertThat(s.getDurationS()).isEqualTo(0);
        assertThat(s.getAvgSpeedMps()).isEqualTo(0d);
        assertThat(s.getMaxSpeedMps()).isEqualTo(0d);
        assertThat(s.getPolyline()).isEqualTo("");
        assertThat(s.getSimplifiedPolyline()).isEqualTo("");
    }

    @Test
    void buildOrRebuild_calculatesDistanceAndSpeed() {
        UUID id = UUID.randomUUID();
        Instant t0 = Instant.now();
        Instant t1 = t0.plusSeconds(10);
        var session = TrackingSessionEntity.builder()
                .id(id)
                .startTime(t0)
                .stopTime(t1)
                .build();

        TrackingPointEntity p1 = TrackingPointEntity.builder()
                .point(gf.createPoint(new Coordinate(69.2797, 41.3111)))
                .deviceTimestamp(t0)
                .speedMps(3.0f)
                .build();
        TrackingPointEntity p2 = TrackingPointEntity.builder()
                .point(gf.createPoint(new Coordinate(69.2807, 41.3121)))
                .deviceTimestamp(t1)
                .speedMps(4.0f)
                .build();
        when(pointRepository.findBySessionIdOrderByDeviceTimestampAsc(id)).thenReturn(List.of(p1, p2));
        when(summaryRepository.findById(id)).thenReturn(Optional.empty());
        when(summaryRepository.save(any(SessionSummaryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.buildOrRebuild(session);

        ArgumentCaptor<SessionSummaryEntity> captor = ArgumentCaptor.forClass(SessionSummaryEntity.class);
        verify(summaryRepository).save(captor.capture());
        SessionSummaryEntity summary = captor.getValue();
        assertThat(summary.getDistanceM()).isGreaterThan(0d);
        assertThat(summary.getDurationS()).isEqualTo(10);
        assertThat(summary.getAvgSpeedMps()).isGreaterThan(0d);
        assertThat(summary.getMaxSpeedMps()).isEqualTo(4.0);
        assertThat(summary.getPolyline()).isNotBlank();
        assertThat(summary.getSimplifiedPolyline()).isNotBlank();
    }
}
