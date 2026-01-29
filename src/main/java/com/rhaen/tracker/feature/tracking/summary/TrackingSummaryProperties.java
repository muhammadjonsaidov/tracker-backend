package com.rhaen.tracker.feature.tracking.summary;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tracking.summary")
public record TrackingSummaryProperties(
        double simplifyEpsilonM,
        int maxPolylinePoints
) {}
