package com.rhaen.tracker.feature.tracking.realtime.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record LastLocationEvent(
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
