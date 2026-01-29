package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationBroadcaster;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.security.jwt.StreamTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stream")
public class AdminStreamController {

    private final LastLocationBroadcaster broadcaster;
    private final LastLocationCache lastLocationCache;
    private final StreamTokenService streamTokenService;

    @GetMapping("/token")
    public ApiResponse<?> streamToken(@AuthenticationPrincipal Jwt jwt) {
        var token = streamTokenService.issue(jwt);
        return ApiResponse.ok(Map.of(
                "token", token.token(),
                "expiresAt", token.expiresAt()
        ));
    }

    @GetMapping(value = "/last-locations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLastLocations() {
        SseEmitter emitter = broadcaster.subscribe();

        // 1) init snapshot (dashboard tez to‘lsin)
        try {
            var init = lastLocationCache.getAll().stream()
                    .map(snap -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("userId", snap.userId());
                        row.put("sessionId", snap.sessionId());
                        row.put("status", snap.status());
                        row.put("active", snap.active());
                        row.put("stale", lastLocationCache.isStale(snap));
                        row.put("ts", snap.ts());
                        row.put("lat", snap.lat());
                        row.put("lon", snap.lon());
                        row.put("accuracyM", snap.accuracyM());
                        row.put("speedMps", snap.speedMps());
                        return row;
                    })
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
