package com.rhaen.tracker.config;

import com.rhaen.tracker.feature.tracking.expiry.SessionExpiryProperties;
import com.rhaen.tracker.feature.tracking.history.TrackingHistoryProperties;
import com.rhaen.tracker.feature.tracking.ingest.TrackingIngestProperties;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationProperties;
import com.rhaen.tracker.feature.tracking.retention.RetentionProperties;
import com.rhaen.tracker.feature.tracking.summary.TrackingSummaryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        LastLocationProperties.class,
        TrackingSummaryProperties.class,
        TrackingHistoryProperties.class,
        SessionExpiryProperties.class,
        TrackingIngestProperties.class,
        RetentionProperties.class
})
public class TrackingRealtimeConfig {}
