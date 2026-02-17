package com.rhaen.tracker.feature.tracking.ingest;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redis;
    private final TrackingIngestProperties props;

    // returns [allowed(1/0), current]
    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local ttl = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local incr = tonumber(ARGV[3])

            local current = redis.call('INCRBY', key, incr)
            if current == incr then
              redis.call('EXPIRE', key, ttl)
            end

            if current > limit then
              return {0, current}
            end
            return {1, current}
            """,
            List.class
    );

    public RateLimitResult consumePoints(UUID userId, int points) {
        long window = props.windowSeconds();
        long minuteBucket = Instant.now().getEpochSecond() / window;

        String key = "rl:points:" + userId + ":" + minuteBucket;

        List res = redis.execute(
                SCRIPT,
                List.of(key),
                String.valueOf(window + 5),          // ttl a little > window
                String.valueOf(props.pointsPerMinute()),
                String.valueOf(points)
        );

        if (res == null || res.size() < 2) {
            // Redis down bo‘lsa MVP’da “allow” qilish mumkin (fail-open).
            return new RateLimitResult(true, -1);
        }

        boolean allowed = ((Number) res.get(0)).intValue() == 1;
        long current = ((Number) res.get(1)).longValue();
        return new RateLimitResult(allowed, current);
    }

    public record RateLimitResult(boolean allowed, long current) {}
}
