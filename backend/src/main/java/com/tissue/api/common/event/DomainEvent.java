package com.tissue.api.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;

public interface DomainEvent {

	UUID getEventId();

	LocalDateTime getOccurredAt();

	NotificationType getNotificationType();

	ResourceType getEntityType();

	Long getEntityId();

	String getEntityKey();

	String getWorkspaceCode();

	Long getTriggeredByWorkspaceMemberId();

	String getType();
}
