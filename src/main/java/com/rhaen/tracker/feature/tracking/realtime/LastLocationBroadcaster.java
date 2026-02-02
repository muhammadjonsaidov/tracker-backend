package com.rhaen.tracker.feature.tracking.realtime;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class LastLocationBroadcaster {
    private final MeterRegistry meterRegistry;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // 0L => timeout yo‘q (server/proxyga bog‘liq). Xohlasang 30 min: 30 * 60 * 1000L
        SseEmitter emitter = new SseEmitter(0L);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    public void broadcastUpdate(Object payload) {
        // payload: Map yoki DTO bo‘lishi mumkin
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("update")
                        .data(payload));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        }
    }

    /** Proxy/LoadBalancer SSE connection’ni o‘chirib qo‘ymasligi uchun heartbeat */
    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data(Instant.now().toString()));
            } catch (IOException ex) {
                emitters.remove(emitter);
            }
        }
    }

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("tracker.sse.connections", emitters, list -> (double) list.size())
                .description("Active SSE connections")
                .register(meterRegistry);
    }

    public int connections() {
        return emitters.size();
    }
}
