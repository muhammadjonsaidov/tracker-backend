package com.rhaen.tracker.feature.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TrackingDtos {

    public record StartSessionResponse(
            @Schema(description = "Session id")
            UUID sessionId,
            @Schema(description = "Session start time (UTC)")
            Instant startTime,
            @Schema(description = "Session status", example = "ACTIVE")
            String status
    ) {}

    public record StopSessionRequest(
            @Schema(description = "Stop time (optional). If null, server uses now()")
            Instant stopTime,
            @Schema(description = "Stop latitude (optional)")
            Double stopLat,
            @Schema(description = "Stop longitude (optional)")
            Double stopLon
    ) {}

    public record LocationPoint(
            @Schema(description = "Client event id for de-duplication (optional)")
            UUID eventId,

            @NotNull
            @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
            @Schema(description = "Latitude", example = "41.3111")
            Double lat,

            @NotNull
            @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
            @Schema(description = "Longitude", example = "69.2797")
            Double lon,

            @NotNull
            @Schema(description = "Device timestamp (UTC)")
            Instant deviceTimestamp,

            @Schema(description = "Accuracy meters (optional)")
            Float accuracyM,
            @Schema(description = "Speed m/s (optional)")
            Float speedMps,
            @Schema(description = "Heading degrees (optional)")
            Float headingDeg,
            @Schema(description = "Provider (gps/network/fused)")
            String provider,
            @Schema(description = "Mock location flag")
            Boolean mock
    ) {}

    public record IngestPointsRequest(
            @Schema(description = "List of location points")
            @NotEmpty List<LocationPoint> points
    ) {}

    public record IngestResponse(
            @Schema(description = "Points accepted in request")
            int accepted,
            @Schema(description = "Points inserted into DB")
            int inserted
    ) {}

    public record PointRow(
            @Schema(description = "Timestamp (UTC)")
            Instant ts,
            @Schema(description = "Latitude")
            double lat,
            @Schema(description = "Longitude")
            double lon,
            @Schema(description = "Accuracy meters (optional)")
            Float accuracyM,
            @Schema(description = "Speed m/s (optional)")
            Float speedMps,
            @Schema(description = "Heading degrees (optional)")
            Float headingDeg
    ) {}

    public record SessionRow(
            @Schema(description = "Session id")
            UUID sessionId,
            @Schema(description = "Start time (UTC)")
            Instant startTime,
            @Schema(description = "Stop time (UTC)")
            Instant stopTime,
            @Schema(description = "Status")
            String status,
            @Schema(description = "Last point timestamp")
            Instant lastPointAt
    ) {}

    public record SessionDetailsResponse(
            @Schema(description = "Session id")
            UUID sessionId,
            @Schema(description = "User id")
            UUID userId,
            @Schema(description = "Start time (UTC)")
            Instant startTime,
            @Schema(description = "Stop time (UTC)")
            Instant stopTime,
            @Schema(description = "Status")
            String status,
            @Schema(description = "Encoded polyline")
            String polyline
    ) {}
}
