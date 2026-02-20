package com.rhaen.tracker.feature.tracking.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhaen.tracker.feature.tracking.realtime.dto.LastLocationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LastLocationCache {

    private static final String KEY_PREFIX = "lastloc:"; // lastloc:{userId}
    private static final String USERS_SET = "lastloc:users"; // set of userIds

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
        } catch (Exception e) {
            // cache failure should not break ingest
        }
    }

    public void remove(UUID userId) {
        try {
            String key = KEY_PREFIX + userId;
            redis.delete(key);
            redis.opsForSet().remove(USERS_SET, userId.toString());
            // Optionally broadcast a "removal" event if the frontend supports it,
            // but for now, the next full list fetch or stale check will clear it.
        } catch (Exception e) {
            // ignore
        }
    }

    public Optional<LastLocationSnapshot> get(UUID userId) {
        String json = redis.opsForValue().get(KEY_PREFIX + userId);
        if (json == null)
            return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, LastLocationSnapshot.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** For admin dashboard: list all users known to Redis */
    public List<LastLocationSnapshot> getAll() {
        Set<String> ids = redis.opsForSet().members(USERS_SET);
        if (ids == null || ids.isEmpty())
            return List.of();

        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        List<String> jsons = redis.opsForValue().multiGet(keys);
        if (jsons == null)
            return List.of();

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
            } catch (Exception ignore) {
            }
        }

        if (!missingIds.isEmpty()) {
            redis.opsForSet().remove(USERS_SET, missingIds.toArray());
        }

        return result;
    }

    /** Computed flag: ACTIVE but last update is old */
    public boolean isStale(LastLocationSnapshot snap) {
        if (!snap.active())
            return false;
        Instant ts = snap.ts();
        if (ts == null)
            return true;
        return Duration.between(ts, Instant.now()).getSeconds() > props.staleSeconds();
    }

    private LastLocationEvent toAdminPayload(LastLocationSnapshot snap) {
        return new LastLocationEvent(
                snap.userId(),
                snap.sessionId(),
                snap.status(),
                snap.active(),
                isStale(snap),
                snap.ts(),
                snap.lat(),
                snap.lon(),
                snap.accuracyM(),
                snap.speedMps(),
                snap.headingDeg());
    }

}
