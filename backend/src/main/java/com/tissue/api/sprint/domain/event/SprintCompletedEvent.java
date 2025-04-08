package com.tissue.api.sprint.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.sprint.domain.Sprint;

import lombok.Getter;

@Getter
public class SprintCompletedEvent extends SprintEvent {

	public SprintCompletedEvent(
		Long sprintId,
		String sprintKey,
		String workspaceCode,
		Long triggeredByWorkspaceMemberId
	) {
		super(
			NotificationType.SPRINT_COMPLETED,
			ResourceType.SPRINT,
			workspaceCode,
			sprintId,
			sprintKey,
			triggeredByWorkspaceMemberId
		);
	}

	public static SprintCompletedEvent createEvent(
		Sprint sprint,
		Long triggeredByWorkspaceMemberId
	) {
		return new SprintCompletedEvent(
			sprint.getId(),
			sprint.getSprintKey(),
			sprint.getWorkspaceCode(),
			triggeredByWorkspaceMemberId
		);
	}
}
