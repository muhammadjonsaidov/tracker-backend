package com.rhaen.tracker.feature.tracking.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LastLocationCacheTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private LastLocationBroadcaster broadcaster;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private SetOperations<String, String> setOps;

    private LastLocationCache cache;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);
        cache = new LastLocationCache(redis, new ObjectMapper().findAndRegisterModules(), new LastLocationProperties(10, 120), broadcaster);
    }

    @Test
    void upsert_writesRedis_andBroadcasts() {
        LastLocationSnapshot snap = snapshot(true, Instant.now());

        cache.upsert(snap);

        verify(valueOps).set(startsWith("lastloc:"), anyString(), eq(Duration.ofMinutes(10)));
        verify(setOps).add(eq("lastloc:users"), eq(snap.userId().toString()));
        verify(broadcaster).broadcastUpdate(any());
    }

    @Test
    void get_returnsSnapshot_whenJsonValid() throws Exception {
        LastLocationSnapshot snap = snapshot(true, Instant.now());
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(snap);
        when(valueOps.get(anyString())).thenReturn(json);

        Optional<LastLocationSnapshot> found = cache.get(snap.userId());

        assertThat(found).isPresent();
        assertThat(found.get().sessionId()).isEqualTo(snap.sessionId());
    }

    @Test
    void get_returnsEmpty_whenJsonInvalid() {
        when(valueOps.get(anyString())).thenReturn("not-json");

        assertThat(cache.get(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getAll_returnsParsed_andCleansMissingIds() throws Exception {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        Set<String> ids = new LinkedHashSet<>(List.of(u1.toString(), u2.toString()));
        when(setOps.members("lastloc:users")).thenReturn(ids);

        String json1 = new ObjectMapper().findAndRegisterModules().writeValueAsString(snapshotForUser(u1));
        when(valueOps.multiGet(anyList())).thenReturn(java.util.Arrays.asList(json1, null));

        List<LastLocationSnapshot> all = cache.getAll();

        assertThat(all).hasSize(1);
        assertThat(all.getFirst().userId()).isEqualTo(u1);
        verify(setOps).remove(eq("lastloc:users"), any());
    }

    @Test
    void isStale_rules_work() {
        assertThat(cache.isStale(snapshot(false, Instant.now().minusSeconds(1000)))).isFalse();
        assertThat(cache.isStale(snapshot(true, null))).isTrue();
        assertThat(cache.isStale(snapshot(true, Instant.now().minusSeconds(121)))).isTrue();
        assertThat(cache.isStale(snapshot(true, Instant.now().minusSeconds(30)))).isFalse();
    }

    private static LastLocationSnapshot snapshot(boolean active, Instant ts) {
        return new LastLocationSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                active ? "ACTIVE" : "STOPPED",
                active,
                41.0,
                69.0,
                ts,
                3.2f,
                6.4f,
                90.0f
        );
    }

    private static LastLocationSnapshot snapshotForUser(UUID userId) {
        return new LastLocationSnapshot(
                userId,
                UUID.randomUUID(),
                "ACTIVE",
                true,
                41.0,
                69.0,
                Instant.now(),
                null,
                null,
                null
        );
    }
}
