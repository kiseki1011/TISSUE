package com.tissue.feature.member.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record VerificationEmailRequestedEvent(UUID eventId, Instant occurredAt, String email, String verificationLink)
        implements DomainEvent {

    public static VerificationEmailRequestedEvent create(String email, String verificationLink) {
        return new VerificationEmailRequestedEvent(UUID.randomUUID(), Instant.now(), email, verificationLink);
    }
}
