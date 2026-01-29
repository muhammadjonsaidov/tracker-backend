package com.rhaen.tracker.feature.admin.persistence;

import com.rhaen.tracker.feature.user.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;

@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_audit_admin_ts", columnList = "admin_id,created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdminAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private UserEntity admin;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id", columnDefinition = "uuid")
    private java.util.UUID targetId;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
