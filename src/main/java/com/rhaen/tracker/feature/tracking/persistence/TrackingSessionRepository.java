package com.rhaen.tracker.feature.tracking.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingSessionRepository extends JpaRepository<TrackingSessionEntity, UUID> {

    List<TrackingSessionEntity> findByUserIdOrderByStartTimeDesc(UUID userId);

    @Query("select s from TrackingSessionEntity s where s.user.id = :userId and s.status = 'ACTIVE' order by s.startTime desc")
    Optional<TrackingSessionEntity> findActiveByUserId(UUID userId);
}
