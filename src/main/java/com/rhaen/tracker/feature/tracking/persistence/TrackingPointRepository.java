package com.rhaen.tracker.feature.tracking.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrackingPointRepository extends JpaRepository<TrackingPointEntity, Long> {
    List<TrackingPointEntity> findBySessionIdOrderByDeviceTimestampAsc(UUID sessionId);
    long countBySessionId(UUID sessionId);
    long countBySessionIdAndDeviceTimestampBetween(UUID sessionId, Instant from, Instant to);
    List<TrackingPointEntity> findBySessionIdAndDeviceTimestampBetweenOrderByDeviceTimestampAsc(
            UUID sessionId, Instant from, Instant to);
}
