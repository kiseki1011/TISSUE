package com.tissue.feature.realtime.application;

import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically pings open SSE streams so idle connections survive proxy timeouts and dead
 * ones are detected and pruned even when no domain events are flowing.
 */
@Component
@RequiredArgsConstructor
@LLMGenerated(llmInvolvement = LLMInvolvement.VIBE_CODED, agentName = "claude-opus-4-8")
public class SseHeartbeatScheduler {

    private final SseEmitterRegistry registry;

    @Scheduled(fixedRateString = "${tissue.realtime.sse.heartbeat-ms:15000}")
    public void heartbeat() {
        registry.heartbeat();
    }
}
