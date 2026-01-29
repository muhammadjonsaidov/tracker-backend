package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.common.response.ApiResponse;
import com.rhaen.tracker.feature.admin.dto.AdminDtos;
import com.rhaen.tracker.feature.admin.query.AdminQueryService;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationBroadcaster;
import com.rhaen.tracker.security.jwt.StreamTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stream")
@Tag(name = "Admin Stream", description = "Admin SSE stream")
@SecurityRequirement(name = "bearerAuth")
public class AdminStreamController {

    private final LastLocationBroadcaster broadcaster;
    private final AdminQueryService adminQueryService;
    private final StreamTokenService streamTokenService;

    @GetMapping("/token")
    @Operation(summary = "Issue stream token", description = "Returns short-lived token for SSE connection.")
    public ApiResponse<AdminDtos.StreamTokenResponse> streamToken(@AuthenticationPrincipal Jwt jwt) {
        var token = streamTokenService.issue(jwt);
        return ApiResponse.ok(new AdminDtos.StreamTokenResponse(token.token(), token.expiresAt()));
    }

    @GetMapping(value = "/last-locations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream", description = "Stream last location updates in real-time. Use access_token query param (short-lived stream token).")
    public SseEmitter streamLastLocations() {
        SseEmitter emitter = broadcaster.subscribe();

        // 1) init snapshot (dashboard tez to‘lsin)
        try {
            var init = adminQueryService.listLastLocations();
            var payload = new AdminDtos.StreamInitResponse(Instant.now().toString(), init);
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data(payload));
        } catch (Exception e) {
            // init fail bo‘lsa ham connection tursin
        }

        return emitter;
    }
}
