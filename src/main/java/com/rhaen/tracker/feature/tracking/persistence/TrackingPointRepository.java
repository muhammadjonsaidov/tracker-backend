package com.rhaen.tracker.feature.tracking.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrackingPointRepository extends JpaRepository<TrackingPointEntity, Long> {
    List<TrackingPointEntity> findBySessionIdOrderByDeviceTimestampAsc(UUID sessionId);
    long countBySessionId(UUID sessionId);
    long countBySessionIdAndDeviceTimestampBetween(UUID sessionId, Instant from, Instant to);
    long countBySessionIdAndDeviceTimestampGreaterThanEqual(UUID sessionId, Instant from);
    long countBySessionIdAndDeviceTimestampLessThanEqual(UUID sessionId, Instant to);
    List<TrackingPointEntity> findBySessionIdAndDeviceTimestampBetweenOrderByDeviceTimestampAsc(
            UUID sessionId, Instant from, Instant to);
    List<TrackingPointEntity> findBySessionIdAndDeviceTimestampGreaterThanEqualOrderByDeviceTimestampAsc(
            UUID sessionId, Instant from);
    List<TrackingPointEntity> findBySessionIdAndDeviceTimestampLessThanEqualOrderByDeviceTimestampAsc(
            UUID sessionId, Instant to);

    @Query("""
            select p from TrackingPointEntity p
            where p.session.id = :sessionId
              and (:from is null or p.deviceTimestamp >= :from)
              and (:to is null or p.deviceTimestamp <= :to)
            order by p.deviceTimestamp asc
            """)
    List<TrackingPointEntity> findBySessionIdAndDeviceTimestampRange(UUID sessionId, Instant from, Instant to);

    @Query("""
            select count(p) from TrackingPointEntity p
            where p.session.id = :sessionId
              and (:from is null or p.deviceTimestamp >= :from)
              and (:to is null or p.deviceTimestamp <= :to)
            """)
    long countBySessionIdAndDeviceTimestampRange(UUID sessionId, Instant from, Instant to);
}
