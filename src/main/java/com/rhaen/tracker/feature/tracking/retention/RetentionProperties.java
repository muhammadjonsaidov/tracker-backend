package com.rhaen.tracker.feature.tracking.retention;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tracking.retention")
public record RetentionProperties(
        int archiveAfterDays,
        int prunePointsAfterDays,
        long sweepIntervalSeconds,
        int batchSessions,
        int batchPoints,
        boolean runAtStartup
) {}
