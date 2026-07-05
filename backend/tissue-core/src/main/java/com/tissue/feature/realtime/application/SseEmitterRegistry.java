package com.tissue.feature.realtime.application;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Holds the live {@link SseEmitter}s of every subscriber and pushes event messages to them.
 *
 * <p>Keyed by member id (a member may hold several emitters from multiple sessions) so an
 * event can be routed to specific people. Dead emitters are pruned lazily on the first failed send
 * and actively by the heartbeat.
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final long emitterTimeoutMs;

    public SseEmitterRegistry(@Value("${tissue.realtime.sse.timeout-ms:1800000}") long emitterTimeoutMs) {
        this.emitterTimeoutMs = emitterTimeoutMs;
    }

    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        emitters.computeIfAbsent(memberId, key -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(memberId, emitter));
        emitter.onTimeout(() -> remove(memberId, emitter));
        emitter.onError(throwable -> remove(memberId, emitter));
        deliver(memberId, emitter, SseEmitter.event().comment("connected"));
        return emitter;
    }

    public void send(Collection<Long> memberIds, String eventName, String eventId, String data) {
        for (Long memberId : memberIds) {
            Set<SseEmitter> memberEmitters = emitters.get(memberId);
            if (memberEmitters == null) {
                continue;
            }
            for (SseEmitter emitter : memberEmitters) {
                deliver(
                        memberId,
                        emitter,
                        SseEmitter.event().name(eventName).id(eventId).data(data));
            }
        }
    }

    public void heartbeat() {
        emitters.forEach((memberId, memberEmitters) -> {
            for (SseEmitter emitter : memberEmitters) {
                deliver(memberId, emitter, SseEmitter.event().comment("keep-alive"));
            }
        });
    }

    /**
     * Writes one event to a single stream, evicts the subscriber if the write fails.
     *
     * <p>A failed write means that client is already gone or its stream has completed, and there is
     * nothing to recover, so we drop it.
     */
    private void deliver(Long memberId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception e) {
            log.debug("Realtime: dropping dead SSE stream for member {}", memberId, e);
            evict(memberId, emitter, e);
        }
    }

    private void evict(Long memberId, SseEmitter emitter, Exception cause) {
        remove(memberId, emitter);
        try {
            // End the async request now, instead of holding it until timeout
            emitter.completeWithError(cause);
        } catch (Exception ignored) {
            // Already completed
        }
    }

    public boolean hasMember(Long memberId) {
        Set<SseEmitter> memberEmitters = emitters.get(memberId);
        return memberEmitters != null && !memberEmitters.isEmpty();
    }

    /**
     * Completes every open stream when the context closes, so a shutdown ends streams cleanly
     * (clients reconnect).
     */
    @EventListener(ContextClosedEvent.class)
    public void closeAll() {
        emitters.values().stream().flatMap(Set::stream).toList().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // Already closed
            }
        });
        emitters.clear();
    }

    private void remove(Long memberId, SseEmitter emitter) {
        emitters.computeIfPresent(memberId, (key, memberEmitters) -> {
            memberEmitters.remove(emitter);
            return memberEmitters.isEmpty() ? null : memberEmitters;
        });
    }
}
