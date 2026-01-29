package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.feature.tracking.realtime.LastLocationBroadcaster;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stream")
public class AdminStreamController {

    private final LastLocationBroadcaster broadcaster;
    private final LastLocationCache lastLocationCache;

    @GetMapping(value = "/last-locations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLastLocations() {
        SseEmitter emitter = broadcaster.subscribe();

        // 1) init snapshot (dashboard tez to‘lsin)
        try {
            var init = lastLocationCache.getAll().stream()
                    .map(snap -> Map.of(
                            "userId", snap.userId(),
                            "sessionId", snap.sessionId(),
                            "status", snap.status(),
                            "active", snap.active(),
                            "stale", lastLocationCache.isStale(snap),
                            "ts", snap.ts(),
                            "lat", snap.lat(),
                            "lon", snap.lon(),
                            "accuracyM", snap.accuracyM(),
                            "speedMps", snap.speedMps()
                    ))
                    .toList();

            emitter.send(SseEmitter.event()
                    .name("init")
                    .data(Map.of(
                            "serverTs", Instant.now().toString(),
                            "items", init
                    )));
        } catch (Exception e) {
            // init fail bo‘lsa ham connection tursin
        }

        return emitter;
    }
}
