package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.feature.admin.query.AdminQueryService;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationBroadcaster;
import com.rhaen.tracker.security.jwt.StreamTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStreamControllerTest {

    @Mock private LastLocationBroadcaster broadcaster;
    @Mock private AdminQueryService queryService;
    @Mock private StreamTokenService streamTokenService;

    @Test
    void streamToken_returnsShortLivedToken() {
        AdminStreamController controller = new AdminStreamController(broadcaster, queryService, streamTokenService);
        Jwt jwt = new Jwt("src", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"), Map.of("sub", "admin"));
        when(streamTokenService.issue(jwt)).thenReturn(new StreamTokenService.StreamToken("abc", Instant.now().plusSeconds(300)));

        var resp = controller.streamToken(jwt);

        assertThat(resp.data().token()).isEqualTo("abc");
    }

    @Test
    void streamLastLocations_returnsEmitter_evenWhenInitFails() {
        AdminStreamController controller = new AdminStreamController(broadcaster, queryService, streamTokenService);
        SseEmitter emitter = new SseEmitter(0L);
        when(broadcaster.subscribe()).thenReturn(emitter);
        when(queryService.listLastLocations()).thenThrow(new RuntimeException("boom"));

        SseEmitter out = controller.streamLastLocations();

        assertThat(out).isSameAs(emitter);
    }

    @Test
    void streamLastLocations_sendsInit_whenDataAvailable() {
        AdminStreamController controller = new AdminStreamController(broadcaster, queryService, streamTokenService);
        SseEmitter emitter = new SseEmitter(0L);
        when(broadcaster.subscribe()).thenReturn(emitter);
        when(queryService.listLastLocations()).thenReturn(List.of());

        SseEmitter out = controller.streamLastLocations();

        assertThat(out).isSameAs(emitter);
    }
}
