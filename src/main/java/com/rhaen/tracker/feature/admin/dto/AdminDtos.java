package com.rhaen.tracker.feature.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AdminDtos {

    public record UserRow(
            @Schema(description = "User id")
            UUID id,
            @Schema(description = "Username")
            String username,
            @Schema(description = "Email")
            String email,
            @Schema(description = "Role", example = "ADMIN")
            String role,
            @Schema(description = "Created at (UTC)")
            Instant createdAt
    ) {}

    public record SessionRow(
            @Schema(description = "Session id")
            UUID sessionId,
            @Schema(description = "User id")
            UUID userId,
            @Schema(description = "Username")
            String username,
            @Schema(description = "Start time (UTC)")
            Instant startTime,
            @Schema(description = "Stop time (UTC)")
            Instant stopTime,
            @Schema(description = "Status")
            String status,
            @Schema(description = "Last point time")
            Instant lastPointAt
    ) {}

    public record SessionPage(
            @Schema(description = "Items")
            List<SessionRow> items,
            @Schema(description = "Current page index")
            int page,
            @Schema(description = "Page size")
            int size,
            @Schema(description = "Total elements")
            long totalElements,
            @Schema(description = "Total pages")
            int totalPages
    ) {}

    public record Bbox(
            @Schema(description = "Min latitude")
            Double minLat,
            @Schema(description = "Min longitude")
            Double minLon,
            @Schema(description = "Max latitude")
            Double maxLat,
            @Schema(description = "Max longitude")
            Double maxLon
    ) {}

    public record SessionSummaryResponse(
            @Schema(description = "Session id")
            UUID sessionId,
            @Schema(description = "Encoded polyline (full)")
            String polyline,
            @Schema(description = "Encoded polyline (simplified)")
            String simplifiedPolyline,
            @Schema(description = "Total distance meters")
            Double distanceM,
            @Schema(description = "Duration seconds")
            Integer durationS,
            @Schema(description = "Average speed m/s")
            Double avgSpeedMps,
            @Schema(description = "Max speed m/s")
            Double maxSpeedMps,
            @Schema(description = "Points count")
            Integer pointsCount,
            @Schema(description = "Bounding box")
            Bbox bbox,
            @Schema(description = "Raw points pruned timestamp (optional)")
            Instant rawPointsPrunedAt
    ) {}

    public record LastLocationRow(
            @Schema(description = "User id")
            UUID userId,
            @Schema(description = "Session id")
            UUID sessionId,
            @Schema(description = "Status")
            String status,
            @Schema(description = "Tracking active")
            boolean active,
            @Schema(description = "Stale flag")
            boolean stale,
            @Schema(description = "Timestamp (UTC)")
            Instant ts,
            @Schema(description = "Latitude")
            Double lat,
            @Schema(description = "Longitude")
            Double lon,
            @Schema(description = "Accuracy meters")
            Float accuracyM,
            @Schema(description = "Speed m/s")
            Float speedMps,
            @Schema(description = "Heading degrees")
            Float headingDeg
    ) {}

    public record StreamTokenResponse(
            @Schema(description = "Short-lived stream token")
            String token,
            @Schema(description = "Token expiration time (UTC)")
            Instant expiresAt
    ) {}

    public record StreamInitResponse(
            @Schema(description = "Server timestamp (UTC)")
            String serverTs,
            @Schema(description = "Initial items")
            List<LastLocationRow> items
    ) {}

    public record AuditLogRow(
            @Schema(description = "Audit log id")
            Long id,
            @Schema(description = "Actor user id")
            UUID adminId,
            @Schema(description = "Actor username")
            String adminUsername,
            @Schema(description = "Action")
            String action,
            @Schema(description = "Target type")
            String targetType,
            @Schema(description = "Target id")
            UUID targetId,
            @Schema(description = "Metadata JSON")
            String metadata,
            @Schema(description = "Source IP")
            String ipAddress,
            @Schema(description = "User agent")
            String userAgent,
            @Schema(description = "Created at (UTC)")
            Instant createdAt
    ) {}
}
