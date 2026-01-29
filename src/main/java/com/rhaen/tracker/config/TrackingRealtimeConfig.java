package com.rhaen.tracker.config;

import com.rhaen.tracker.feature.tracking.realtime.LastLocationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LastLocationProperties.class)
public class TrackingRealtimeConfig {}
