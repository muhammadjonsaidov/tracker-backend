package com.rhaen.tracker.feature.tracking.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LastLocationCache {

    private static final String KEY_PREFIX = "lastloc:";      // lastloc:{userId}
    private static final String USERS_SET  = "lastloc:users"; // set of userIds

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final LastLocationProperties props;
    private final LastLocationBroadcaster broadcaster;

    public void upsert(LastLocationSnapshot snap) {
        try {
            String key = KEY_PREFIX + snap.userId();
            String json = objectMapper.writeValueAsString(snap);

            redis.opsForValue().set(key, json, Duration.ofMinutes(props.ttlMinutes()));
            redis.opsForSet().add(USERS_SET, snap.userId().toString());

            // 🔴 realtime push (admin stream)
            broadcaster.broadcastUpdate(toAdminPayload(snap));
        } catch (JsonProcessingException e) {
            // cache failure should not break ingest
        }
    }

    public Optional<LastLocationSnapshot> get(UUID userId) {
        String json = redis.opsForValue().get(KEY_PREFIX + userId);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, LastLocationSnapshot.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** For admin dashboard: list all users known to Redis */
    public List<LastLocationSnapshot> getAll() {
        Set<String> ids = redis.opsForSet().members(USERS_SET);
        if (ids == null || ids.isEmpty()) return List.of();

        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        List<String> jsons = redis.opsForValue().multiGet(keys);
        if (jsons == null) return List.of();

        List<LastLocationSnapshot> result = new ArrayList<>(jsons.size());
        List<String> missingIds = new ArrayList<>();

        int i = 0;
        for (String id : ids) {
            String json = jsons.get(i++);
            if (json == null) {
                missingIds.add(id); // TTL expired, cleanup set
                continue;
            }
            try {
                result.add(objectMapper.readValue(json, LastLocationSnapshot.class));
            } catch (Exception ignore) { }
        }

        if (!missingIds.isEmpty()) {
            redis.opsForSet().remove(USERS_SET, missingIds.toArray());
        }

        return result;
    }

    /** Computed flag: ACTIVE but last update is old */
    public boolean isStale(LastLocationSnapshot snap) {
        if (!snap.active()) return false;
        Instant ts = snap.ts();
        if (ts == null) return true;
        return Duration.between(ts, Instant.now()).getSeconds() > props.staleSeconds();
    }

    private Map<String, Object> toAdminPayload(LastLocationSnapshot snap) {
        return Map.of(
                "userId", snap.userId(),
                "sessionId", snap.sessionId(),
                "status", snap.status(),
                "active", snap.active(),
                "ts", snap.ts(),
                "lat", snap.lat(),
                "lon", snap.lon(),
                "accuracyM", snap.accuracyM(),
                "speedMps", snap.speedMps(),
                "headingDeg", snap.headingDeg()
        );
    }

}
