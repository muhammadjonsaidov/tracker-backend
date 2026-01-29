package com.rhaen.tracker.feature.tracking.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_summary")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SessionSummaryEntity {

    @Id
    @Column(name = "session_id", columnDefinition = "uuid")
    private UUID sessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "session_id")
    private TrackingSessionEntity session;

    @Column(columnDefinition = "text")
    private String polyline;

    @Column(name = "simplified_polyline", columnDefinition = "text")
    private String simplifiedPolyline;

    @Column(name = "points_count")
    private Integer pointsCount;

    @Column(name = "distance_m")
    private Double distanceM;

    @Column(name = "duration_s")
    private Integer durationS;

    @Column(name = "avg_speed_mps")
    private Double avgSpeedMps;

    @Column(name = "max_speed_mps")
    private Double maxSpeedMps;

    @Column(name = "start_point", columnDefinition = "geography(Point,4326)")
    private Point startPoint;

    @Column(name = "end_point", columnDefinition = "geography(Point,4326)")
    private Point endPoint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "bbox_min_lat")
    private Double bboxMinLat;

    @Column(name = "bbox_min_lon")
    private Double bboxMinLon;

    @Column(name = "bbox_max_lat")
    private Double bboxMaxLat;

    @Column(name = "bbox_max_lon")
    private Double bboxMaxLon;
}
