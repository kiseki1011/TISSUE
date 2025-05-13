package com.tissue.api.workspace.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.vo.EntityReference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class WorkspaceEvent implements DomainEvent {

	private final UUID eventId = UUID.randomUUID();
	private final LocalDateTime occurredAt = LocalDateTime.now();

	private final NotificationType notificationType;
	private final ResourceType entityType;

	private final String workspaceCode;
	private final Long actorMemberId;

	@Override
	public String getEntityKey() {
		return workspaceCode;
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forWorkspace(getWorkspaceCode());
	}
}
