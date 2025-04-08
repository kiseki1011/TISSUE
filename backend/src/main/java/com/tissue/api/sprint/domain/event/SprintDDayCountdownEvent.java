package com.tissue.api.sprint.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.sprint.domain.Sprint;

import lombok.Getter;

@Getter
public class SprintDDayCountdownEvent extends SprintEvent {

	private final int dDay;

	public SprintDDayCountdownEvent(
		Long sprintId,
		String sprintKey,
		String workspaceCode,
		Long triggeredByWorkspaceMemberId,
		int dDay
	) {
		super(
			NotificationType.SPRINT_D_DAY_COUNTDOWN,
			ResourceType.SPRINT,
			workspaceCode,
			sprintId,
			sprintKey,
			triggeredByWorkspaceMemberId
		);
		this.dDay = dDay;
	}

	public static SprintDDayCountdownEvent createEvent(
		Sprint sprint,
		Long triggeredByWorkspaceMemberId,
		int dDay
	) {
		return new SprintDDayCountdownEvent(
			sprint.getId(),
			sprint.getSprintKey(),
			sprint.getWorkspaceCode(),
			triggeredByWorkspaceMemberId,
			dDay
		);
	}
}
