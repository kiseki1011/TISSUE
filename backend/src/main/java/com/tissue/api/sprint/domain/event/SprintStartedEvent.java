package com.tissue.api.sprint.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.sprint.domain.model.Sprint;

import lombok.Getter;

@Getter
public class SprintStartedEvent extends SprintEvent {

	public SprintStartedEvent(
		Long sprintId,
		String sprintKey,
		String workspaceCode,
		Long actorMemberId
	) {
		super(
			NotificationType.SPRINT_STARTED,
			ResourceType.SPRINT,
			workspaceCode,
			sprintId,
			sprintKey,
			actorMemberId
		);
	}

	public static SprintStartedEvent createEvent(
		Sprint sprint,
		Long actorMemberId
	) {
		return new SprintStartedEvent(
			sprint.getId(),
			sprint.getKey(),
			sprint.getWorkspaceCode(),
			actorMemberId
		);
	}
}
