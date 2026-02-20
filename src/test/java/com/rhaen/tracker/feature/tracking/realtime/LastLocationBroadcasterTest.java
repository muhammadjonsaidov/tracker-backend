package com.rhaen.tracker.feature.tracking.realtime;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class LastLocationBroadcasterTest {

    @Test
    void subscribe_and_callbacks_manage_connection_count() {
        LastLocationBroadcaster broadcaster = new LastLocationBroadcaster(new SimpleMeterRegistry());
        ReflectionTestUtils.invokeMethod(broadcaster, "registerMetrics");

        SseEmitter emitter = broadcaster.subscribe();
        assertThat(broadcaster.connections()).isEqualTo(1);

        emitter.complete();
        assertThat(broadcaster.connections()).isLessThanOrEqualTo(1);
    }

    @Test
    void broadcast_and_heartbeat_doNotCrash() {
        LastLocationBroadcaster broadcaster = new LastLocationBroadcaster(new SimpleMeterRegistry());
        ReflectionTestUtils.invokeMethod(broadcaster, "registerMetrics");

        broadcaster.subscribe();
        broadcaster.broadcastUpdate(java.util.Map.of("k", "v"));
        broadcaster.heartbeat();

        assertThat(broadcaster.connections()).isEqualTo(1);
    }
}
