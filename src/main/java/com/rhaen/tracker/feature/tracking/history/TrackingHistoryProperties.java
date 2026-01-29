package com.rhaen.tracker.feature.tracking.history;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tracking.history")
public record TrackingHistoryProperties(
        int defaultMaxPoints,
        int hardLimitPoints
) {}
