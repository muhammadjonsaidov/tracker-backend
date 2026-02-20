package com.rhaen.tracker.feature.tracking.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingSessionRepository extends JpaRepository<TrackingSessionEntity, UUID> {

    List<TrackingSessionEntity> findByUser_IdOrderByStartTimeDesc(UUID userId);
    Page<TrackingSessionEntity> findByUser_IdOrderByStartTimeDesc(UUID userId, Pageable pageable);

    @Query("select s from TrackingSessionEntity s where s.user.id = :userId and s.status = 'ACTIVE' order by s.startTime desc")
    Optional<TrackingSessionEntity> findActiveByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    @Query("""
select s from TrackingSessionEntity s
where (:userId is null or s.user.id = :userId)
  and (:status is null or s.status = :status)
  and s.startTime >= coalesce(:from, s.startTime)
  and s.startTime <= coalesce(:to, s.startTime)
""")
    Page<TrackingSessionEntity> search(UUID userId,
                                       TrackingSessionEntity.Status status,
                                       Instant from,
                                       Instant to,
                                       Pageable pageable);

    Page<TrackingSessionEntity> findByStatusAndLastPointAtBefore(
            TrackingSessionEntity.Status status, Instant cutoff, Pageable pageable
    );

    Page<TrackingSessionEntity> findByStatusAndLastPointAtIsNullAndStartTimeBefore(
            TrackingSessionEntity.Status status, Instant cutoff, Pageable pageable
    );
}
