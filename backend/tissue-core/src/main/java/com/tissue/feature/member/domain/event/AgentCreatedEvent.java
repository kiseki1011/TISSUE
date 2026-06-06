package com.tissue.feature.member.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record AgentCreatedEvent(UUID eventId, Instant occurredAt, Long agentMemberId) implements DomainEvent {

    public static AgentCreatedEvent create(Long agentMemberId) {
        return new AgentCreatedEvent(UUID.randomUUID(), Instant.now(), agentMemberId);
    }
}
