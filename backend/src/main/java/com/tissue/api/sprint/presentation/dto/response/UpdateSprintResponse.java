package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.sprint.domain.Sprint;

import lombok.Builder;

@Builder
public record UpdateSprintResponse(
	Long id,
	String sprintKey,
	String title,
	String goal,
	LocalDateTime plannedStartDate,
	LocalDateTime plannedEndDate,
	LocalDateTime updatedAt,
	Long updatedBy
) {
	public static UpdateSprintResponse from(Sprint sprint) {
		return UpdateSprintResponse.builder()
			.id(sprint.getId())
			.sprintKey(sprint.getSprintKey())
			.title(sprint.getTitle())
			.goal(sprint.getGoal())
			.plannedStartDate(sprint.getPlannedStartDate())
			.plannedEndDate(sprint.getPlannedEndDate())
			.updatedAt(sprint.getLastModifiedDate())
			.updatedBy(sprint.getLastModifiedBy())
			.build();
	}
}
