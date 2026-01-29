package com.rhaen.tracker.feature.tracking.expiry;

import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationSnapshot;
import com.rhaen.tracker.feature.tracking.summary.SessionSummaryService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class SessionExpiryJob {

    private final TrackingSessionRepository sessionRepository;
    private final SessionSummaryService sessionSummaryService;
    private final LastLocationCache lastLocationCache;
    private final SessionExpiryProperties props;
    private final MeterRegistry meterRegistry;

    private Counter expiredCounter;

    @PostConstruct
    void init() {
        expiredCounter = meterRegistry.counter("tracker.session.expired.total");
    }

    @Scheduled(fixedDelayString = "${app.tracking.session.sweep-interval-seconds:60}000")
    @Transactional
    public void sweep() {
        Instant now = Instant.now();

        // 1) last_point_at bor ACTIVE sessionlar
        Instant cutoff = now.minus(props.expireSeconds(), ChronoUnit.SECONDS);
        var page = sessionRepository.findByStatusAndLastPointAtBefore(
                TrackingSessionEntity.Status.ACTIVE, cutoff,
                PageRequest.of(0, props.sweepBatchSize())
        );
        page.getContent().forEach(s -> expireSession(s, now));

        // 2) umuman point kelmagan ACTIVE sessionlar
        Instant cutoffNoPoint = now.minus(props.noPointExpireSeconds(), ChronoUnit.SECONDS);
        var page2 = sessionRepository.findByStatusAndLastPointAtIsNullAndStartTimeBefore(
                TrackingSessionEntity.Status.ACTIVE, cutoffNoPoint,
                PageRequest.of(0, props.sweepBatchSize())
        );
        page2.getContent().forEach(s -> expireSession(s, now));
    }

    private void expireSession(TrackingSessionEntity session, Instant now) {
        // Idempotent: agar boshqa thread/flow STOPPED qilib yuborgan bo‘lsa
        if (session.getStatus() != TrackingSessionEntity.Status.ACTIVE) return;

        session.setStatus(TrackingSessionEntity.Status.EXPIRED);
        session.setStopTime(now);

        // stopPoint: lastPoint bo‘lsa shuni, bo‘lmasa startPoint
        Point p = session.getLastPoint() != null ? session.getLastPoint() : session.getStartPoint();
        if (p != null) session.setStopPoint(p);

        session.setUpdatedAt(now);
        sessionRepository.save(session);

        // Summary finalize (polyline/distance/duration/bbox)
        sessionSummaryService.buildOrRebuild(session);

        // Redis last_location: active=false, status=EXPIRED
        if (p != null) {
            lastLocationCache.upsert(new LastLocationSnapshot(
                    session.getUser().getId(),
                    session.getId(),
                    session.getStatus().name(),
                    false,
                    p.getY(), // lat
                    p.getX(), // lon
                    session.getLastPointAt() != null ? session.getLastPointAt() : now,
                    null, null, null
            ));
        }

        expiredCounter.increment();
    }
}
