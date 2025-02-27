package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.sprint.domain.Sprint;

import lombok.Builder;

@Builder
public record UpdateSprintResponse(
	Long id,
	String sprintKey,
	String title,
	String goal,
	LocalDate startDate,
	LocalDate endDate,
	LocalDateTime updatedAt,
	Long updatedBy
) {
	public static UpdateSprintResponse from(Sprint sprint) {
		return UpdateSprintResponse.builder()
			.id(sprint.getId())
			.sprintKey(sprint.getSprintKey())
			.title(sprint.getTitle())
			.goal(sprint.getGoal())
			.startDate(sprint.getStartDate())
			.endDate(sprint.getEndDate())
			.updatedAt(sprint.getLastModifiedDate())
			.updatedBy(sprint.getLastModifiedByWorkspaceMember())
			.build();
	}
}
