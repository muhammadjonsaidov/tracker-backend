package com.rhaen.tracker.feature.tracking.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracking_points",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_points_session_event", columnNames = {"session_id","event_id"})
        },
        indexes = {
                @Index(name = "idx_points_session_device_ts", columnList = "session_id,device_timestamp"),
                @Index(name = "idx_points_received_at", columnList = "received_at")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TrackingPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TrackingSessionEntity session;

    @Column(name = "event_id", nullable = false, columnDefinition = "uuid")
    private UUID eventId;

    @Column(name = "device_timestamp", nullable = false)
    private Instant deviceTimestamp;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point point;

    @Column(name = "accuracy_m")
    private Float accuracyM;

    @Column(name = "speed_mps")
    private Float speedMps;

    @Column(name = "heading_deg")
    private Float headingDeg;

    @Column(length = 16)
    private String provider;

    @Column(name = "is_mock", nullable = false)
    private boolean mock;
}
