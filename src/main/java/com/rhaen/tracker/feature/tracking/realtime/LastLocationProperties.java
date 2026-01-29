package com.rhaen.tracker.feature.tracking.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tracking.last-location")
public record LastLocationProperties(
        long ttlMinutes,
        long staleSeconds
) {}
