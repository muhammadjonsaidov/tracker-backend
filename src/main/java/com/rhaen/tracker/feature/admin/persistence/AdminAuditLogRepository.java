package com.rhaen.tracker.feature.admin.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, Long> {
    @EntityGraph(attributePaths = "admin")
    Page<AdminAuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
