package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.enums.SprintStatus;

import lombok.Builder;

@Builder
public record CreateSprintResponse(
	Long id,
	String sprintKey,
	String title,
	String goal,
	LocalDateTime plannedStartDate,
	LocalDateTime plannedEndDate,
	SprintStatus status,
	LocalDateTime createdAt,
	Long createdBy
) {
	public static CreateSprintResponse from(Sprint sprint) {
		return CreateSprintResponse.builder()
			.id(sprint.getId())
			.sprintKey(sprint.getSprintKey())
			.title(sprint.getTitle())
			.goal(sprint.getGoal())
			.plannedStartDate(sprint.getPlannedStartDate())
			.plannedEndDate(sprint.getPlannedEndDate())
			.status(sprint.getStatus())
			.createdAt(sprint.getCreatedDate())
			.createdBy(sprint.getCreatedBy())
			.build();
	}
}
