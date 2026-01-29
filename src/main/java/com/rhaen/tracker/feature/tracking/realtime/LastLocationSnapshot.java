package com.rhaen.tracker.feature.tracking.realtime;

import java.time.Instant;
import java.util.UUID;

public record LastLocationSnapshot(
        UUID userId,
        UUID sessionId,
        String status,     // ACTIVE/STOPPED/ARCHIVED
        boolean active,    // tracking ON/OFF
        double lat,
        double lon,
        Instant ts,
        Float accuracyM,
        Float speedMps,
        Float headingDeg
) {}
