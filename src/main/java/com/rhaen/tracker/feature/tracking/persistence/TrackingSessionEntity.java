package com.rhaen.tracker.feature.tracking.persistence;

import com.rhaen.tracker.feature.user.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracking_sessions",
        indexes = {
                @Index(name = "idx_tracking_sessions_user_start", columnList = "user_id,start_time"),
                @Index(name = "idx_tracking_sessions_status", columnList = "status")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TrackingSessionEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "stop_time")
    private Instant stopTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "last_point_at")
    private Instant lastPointAt;

    @Column(name = "start_point", columnDefinition = "geography(Point,4326)")
    private Point startPoint;

    @Column(name = "stop_point", columnDefinition = "geography(Point,4326)")
    private Point stopPoint;

    @Column(name = "last_point", columnDefinition = "geography(Point,4326)")
    private Point lastPoint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Status { ACTIVE, STOPPED, ARCHIVED, EXPIRED, ABORTED }
}
