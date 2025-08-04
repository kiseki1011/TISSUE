package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;

import lombok.Builder;

@Builder
public record SprintDetail(
	Long id,
	String sprintKey,
	String title,
	String goal,
	LocalDateTime plannedStartDate,
	LocalDateTime plannedEndDate,
	SprintStatus status,
	List<String> issueKeys,
	LocalDateTime createdAt,
	Long createdBy
) {
	public static SprintDetail from(Sprint sprint) {
		return SprintDetail.builder()
			.id(sprint.getId())
			.sprintKey(sprint.getKey())
			.title(sprint.getTitle())
			.goal(sprint.getGoal())
			.plannedStartDate(sprint.getPlannedStartDate())
			.plannedEndDate(sprint.getPlannedEndDate())
			.status(sprint.getStatus())
			.issueKeys(sprint.getSprintIssues().stream()
				.map(si -> si.getIssue().getKey())
				.sorted()
				.toList())
			.createdAt(sprint.getCreatedDate())
			.createdBy(sprint.getCreatedBy())
			.build();
	}
}
