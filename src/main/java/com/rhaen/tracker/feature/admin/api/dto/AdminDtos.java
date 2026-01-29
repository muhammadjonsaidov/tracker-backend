package com.rhaen.tracker.feature.admin.api.dto;

import java.time.Instant;
import java.util.UUID;

public class AdminDtos {

    public record UserRow(
            UUID id,
            String username,
            String email,
            String role,
            Instant createdAt
    ) {}

    public record SessionRow(
            UUID sessionId,
            UUID userId,
            String username,
            Instant startTime,
            Instant stopTime,
            String status,
            Instant lastPointAt
    ) {}

    public record PointRow(
            Instant ts,
            double lat,
            double lon,
            Float accuracyM,
            Float speedMps,
            Float headingDeg
    ) {}
}
