package com.tissue.api.sprint.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.notification.domain.model.enums.NotificationType;
import com.tissue.api.notification.domain.model.enums.ResourceType;
import com.tissue.api.notification.domain.vo.EntityReference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class SprintEvent implements DomainEvent {

	private final UUID eventId = UUID.randomUUID();
	private final LocalDateTime occurredAt = LocalDateTime.now();

	private final NotificationType notificationType;
	private final ResourceType resourceType;

	private final String workspaceCode;
	private final Long sprintId;
	private final String sprintKey;
	private final Long actorMemberId;

	@Override
	public String getEntityKey() {
		return sprintKey;
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forSprint(workspaceCode, sprintKey);
	}
}
