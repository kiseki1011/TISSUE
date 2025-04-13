package com.tissue.api.sprint.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.sprint.domain.Sprint;

import lombok.Getter;

@Getter
public class SprintStartedEvent extends SprintEvent {

	public SprintStartedEvent(
		Long sprintId,
		String sprintKey,
		String workspaceCode,
		Long triggeredByWorkspaceMemberId
	) {
		super(
			NotificationType.SPRINT_STARTED,
			ResourceType.SPRINT,
			workspaceCode,
			sprintId,
			sprintKey,
			triggeredByWorkspaceMemberId
		);
	}

	public static SprintStartedEvent createEvent(
		Sprint sprint,
		Long triggeredByWorkspaceMemberId
	) {
		return new SprintStartedEvent(
			sprint.getId(),
			sprint.getSprintKey(),
			sprint.getWorkspaceCode(),
			triggeredByWorkspaceMemberId
		);
	}
}
