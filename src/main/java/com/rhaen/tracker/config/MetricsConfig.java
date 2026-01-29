package com.rhaen.tracker.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("service", "tracker-backend");
    }

    /**
     * ixtiyoriy: juda shovqinli metriclarni kesish
     */
    @Bean
    MeterFilter meterFilter() {
        return MeterFilter.deny(id -> id.getName().startsWith("jvm.threads")); // xohlasang o‘chir
    }
}
