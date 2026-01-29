package com.rhaen.tracker.feature.tracking.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessionSummaryRepository extends JpaRepository<SessionSummaryEntity, UUID> {
}
