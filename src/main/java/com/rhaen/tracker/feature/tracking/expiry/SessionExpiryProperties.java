package com.rhaen.tracker.feature.tracking.expiry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tracking.session")
public record SessionExpiryProperties(
        long expireSeconds,
        long noPointExpireSeconds,
        long sweepIntervalSeconds,
        int sweepBatchSize
) {}
