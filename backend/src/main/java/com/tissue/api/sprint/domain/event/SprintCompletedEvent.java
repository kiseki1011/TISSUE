package com.tissue.api.sprint.domain.event;

import java.time.Instant;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.sprint.domain.model.Sprint;

import lombok.Getter;

@Getter
public class SprintCompletedEvent extends SprintEvent {

	private final Instant sprintStartedAt;
	private final Instant sprintCompletedAt;

	public SprintCompletedEvent(
		Long sprintId,
		String sprintKey,
		String workspaceCode,
		Long actorMemberId,
		Instant sprintStartedAt,
		Instant sprintCompletedAt
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
			sprint.getKey(),
			sprint.getWorkspaceKey(),
			actorMemberId,
			sprint.getStartDate(),
			sprint.getEndDate()
		);
	}
}
