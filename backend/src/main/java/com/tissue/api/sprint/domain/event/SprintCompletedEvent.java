package com.tissue.api.sprint.domain.event;

import java.time.LocalDateTime;

import com.tissue.api.notification.domain.model.enums.NotificationType;
import com.tissue.api.notification.domain.model.enums.ResourceType;
import com.tissue.api.sprint.domain.model.Sprint;

import lombok.Getter;

@Getter
public class SprintCompletedEvent extends SprintEvent {

	private final LocalDateTime sprintStartedAt;
	private final LocalDateTime sprintCompletedAt;

	public SprintCompletedEvent(
		Long sprintId,
		String sprintKey,
		String workspaceCode,
		Long actorMemberId,
		LocalDateTime sprintStartedAt,
		LocalDateTime sprintCompletedAt
	) {
		super(
			NotificationType.SPRINT_COMPLETED,
			ResourceType.SPRINT,
			workspaceCode,
			sprintId,
			sprintKey,
			actorMemberId
		);

		this.sprintStartedAt = sprintStartedAt;
		this.sprintCompletedAt = sprintCompletedAt;
	}

	public static SprintCompletedEvent createEvent(
		Sprint sprint,
		Long actorMemberId
	) {
		return new SprintCompletedEvent(
			sprint.getId(),
			sprint.getSprintKey(),
			sprint.getWorkspaceCode(),
			actorMemberId,
			sprint.getStartDate(),
			sprint.getEndDate()
		);
	}
}
