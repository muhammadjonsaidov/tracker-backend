package com.rhaen.tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.stream.Stream;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
    public CorsProperties {
        if (allowedOrigins == null) {
            throw new IllegalArgumentException("CORS allowed-origins must be configured with at least one origin.");
        }

        allowedOrigins = allowedOrigins.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .flatMap(origin -> Stream.of(origin.split(",")))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();

        if (allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("CORS allowed-origins must contain at least one non-empty origin.");
        }
    }
}
