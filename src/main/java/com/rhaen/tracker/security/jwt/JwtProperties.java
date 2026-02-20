package com.rhaen.tracker.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenMinutes,
        long streamTokenMinutes
) {
    private static final Logger log = LoggerFactory.getLogger(JwtProperties.class);
    private static final String INSECURE_DEFAULT =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    public JwtProperties {
        boolean jwtEnvMissing = System.getenv("JWT_SECRET") == null || System.getenv("JWT_SECRET").isBlank();
        boolean prodProfileActive = Arrays.stream(
                        System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "").split(","))
                .map(String::trim)
                .anyMatch("prod"::equalsIgnoreCase);

        if (prodProfileActive && jwtEnvMissing) {
            log.warn("JWT_SECRET environment variable is not set while prod profile is active.");
        }

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret is required. Set environment variable JWT_SECRET with at least 32 characters."
            );
        }
        if (INSECURE_DEFAULT.equals(secret)) {
            throw new IllegalArgumentException(
                    "JWT secret must not use insecure default value. Set JWT_SECRET environment variable."
            );
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret is too short. JWT_SECRET must be at least 32 characters."
            );
        }
    }
}
