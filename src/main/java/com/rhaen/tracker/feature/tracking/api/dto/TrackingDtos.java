package com.rhaen.tracker.feature.tracking.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TrackingDtos {

    public record StartSessionResponse(
            UUID sessionId,
            Instant startTime,
            String status
    ) {}

    public record StopSessionRequest(
            Instant stopTime,
            Double stopLat,
            Double stopLon
    ) {}

    public record LocationPoint(
            UUID eventId,
            @NotNull Double lat,
            @NotNull Double lon,
            @NotNull Instant deviceTimestamp,
            Float accuracyM,
            Float speedMps,
            Float headingDeg,
            String provider,
            Boolean mock
    ) {}

    public record IngestPointsRequest(
            @NotEmpty List<LocationPoint> points
    ) {}

    public record SessionDetailsResponse(
            UUID sessionId,
            UUID userId,
            Instant startTime,
            Instant stopTime,
            String status,
            String polyline
    ) {}
}
