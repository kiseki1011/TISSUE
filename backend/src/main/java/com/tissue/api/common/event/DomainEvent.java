package com.tissue.api.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;

public interface DomainEvent {

	UUID getEventId();

	LocalDateTime getOccurredAt();

	NotificationType getNotificationType();

	NotificationEntityType getEntityType();

	Long getEntityId();

	String getEntityKey();

	String getWorkspaceCode();

	Long getTriggeredByWorkspaceMemberId();

	String getType();
}
