package com.rhaen.tracker.feature.tracking.ingest;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisRateLimiterTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private TrackingIngestProperties props;
    @Mock
    private CircuitBreakerFactory<?, ?> breakerFactory;
    @Mock
    private CircuitBreaker breaker;
    @Mock
    private CircuitBreakerRegistry registry;

    private RedisRateLimiter limiter;

    @BeforeEach
    void setUp() {
        when(breakerFactory.create("redis")).thenReturn(breaker);
        when(registry.circuitBreaker("redis")).thenReturn(io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("redis"));
        limiter = new RedisRateLimiter(redis, props, breakerFactory, Optional.of(registry));
        limiter.initCircuitBreaker();
    }

    @SuppressWarnings("unchecked")
    @Test
    void consumePoints_allowed() {
        when(props.windowSeconds()).thenReturn(60L);
        when(props.pointsPerMinute()).thenReturn(6000L);
        when(breaker.run(any(Supplier.class), any(Function.class))).thenAnswer(inv -> {
            Supplier<List<?>> supplier = inv.getArgument(0);
            return supplier.get();
        });
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(List.of(1L, 10L));

        var result = limiter.consumePoints(UUID.randomUUID(), 10);

        assertThat(result.allowed()).isTrue();
        assertThat(result.current()).isEqualTo(10L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void consumePoints_rejected() {
        when(props.windowSeconds()).thenReturn(60L);
        when(props.pointsPerMinute()).thenReturn(6000L);
        when(breaker.run(any(Supplier.class), any(Function.class))).thenAnswer(inv -> {
            Supplier<List<?>> supplier = inv.getArgument(0);
            return supplier.get();
        });
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(List.of(0L, 7000L));

        var result = limiter.consumePoints(UUID.randomUUID(), 100);

        assertThat(result.allowed()).isFalse();
        assertThat(result.current()).isEqualTo(7000L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void consumePoints_failOpen_whenCircuitFallbackReturnsNull() {
        when(props.windowSeconds()).thenReturn(60L);
        when(props.pointsPerMinute()).thenReturn(6000L);
        when(breaker.run(any(Supplier.class), any(Function.class))).thenAnswer(inv -> {
            Function<Throwable, List<?>> fallback = inv.getArgument(1);
            return fallback.apply(new RuntimeException("redis down"));
        });

        var result = limiter.consumePoints(UUID.randomUUID(), 25);

        assertThat(result.allowed()).isTrue();
        assertThat(result.current()).isEqualTo(-1L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void consumePoints_usesFixedWindowKeyPrefix() {
        when(props.windowSeconds()).thenReturn(60L);
        when(props.pointsPerMinute()).thenReturn(6000L);
        when(breaker.run(any(Supplier.class), any(Function.class))).thenAnswer(inv -> {
            Supplier<List<?>> supplier = inv.getArgument(0);
            return supplier.get();
        });
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(List.of(1L, 1L));

        limiter.consumePoints(UUID.fromString("11111111-1111-1111-1111-111111111111"), 1);

        ArgumentCaptor<List> keyCaptor = ArgumentCaptor.forClass(List.class);
        verify(redis).execute(any(DefaultRedisScript.class), keyCaptor.capture(), eq("65"), eq("6000"), eq("1"));
        String key = keyCaptor.getValue().getFirst().toString();
        assertThat(key).startsWith("rl:points:11111111-1111-1111-1111-111111111111:");
    }
}
