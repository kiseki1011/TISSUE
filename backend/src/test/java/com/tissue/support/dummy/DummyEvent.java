package com.tissue.support.dummy;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.model.vo.EntityReference;

import lombok.Builder;

public class DummyEvent implements DomainEvent {

	private final Long actorId;
	private final String workspaceCode;
	private final NotificationType type;

	@Builder
	public DummyEvent(Long actorId, String workspaceCode, NotificationType type) {
		this.actorId = actorId;
		this.workspaceCode = workspaceCode;
		this.type = type;
	}

	@Override
	public String getWorkspaceCode() {
		return workspaceCode;
	}

	@Override
	public Long getActorMemberId() {
		return actorId;
	}

	@Override
	public NotificationType getNotificationType() {
		return type;
	}

	@Override
	public String getEntityKey() {
		return null;
	}

	@Override
	public UUID getEventId() {
		return UUID.randomUUID();
	}

	@Override
	public LocalDateTime getOccurredAt() {
		return LocalDateTime.now();
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forWorkspace(workspaceCode);
	}
}
