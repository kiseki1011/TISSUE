package com.tissue.api.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {

	UUID getEventId();

	LocalDateTime getOccurredAt();

	String getType();
}
