package com.tissue.feature.realtime.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.realtime.application.dto.RealtimeMessage;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeBroadcaster {

    private final SseEmitterRegistry registry;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final ObjectMapper objectMapper;

    public void broadcast(
            String category,
            UUID eventId,
            String projectKey,
            @Nullable String issueKey,
            Long actorMemberId,
            Instant occurredAt,
            String type,
            Map<String, Object> data) {
        Set<Long> memberIds = projectMemberQueryRepository.findMemberIdsByProjectKey(projectKey);
        if (memberIds.isEmpty()) {
            return;
        }
        RealtimeMessage message = new RealtimeMessage(type, projectKey, issueKey, actorMemberId, occurredAt, data);
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.warn("Realtime: failed to serialize {} for project {}", type, projectKey, e);
            return;
        }
        registry.send(memberIds, category, eventId.toString(), json);
    }
}
