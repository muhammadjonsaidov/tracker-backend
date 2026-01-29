package com.rhaen.tracker.feature.tracking.ingest;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tracking.ingest")
public record TrackingIngestProperties(
        int maxBatchSize,
        long pointsPerMinute,
        long windowSeconds
) {}
