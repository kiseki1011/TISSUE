package com.tissue.api.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.model.vo.EntityReference;

public interface DomainEvent {

	UUID getEventId();

	LocalDateTime getOccurredAt();

	NotificationType getNotificationType();

	String getEntityKey();

	String getWorkspaceCode();

	Long getActorMemberId();

	EntityReference createEntityReference();
}
