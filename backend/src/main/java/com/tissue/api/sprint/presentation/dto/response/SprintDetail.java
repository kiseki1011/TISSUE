package com.tissue.api.sprint.presentation.dto.response;

import java.time.Instant;
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
	Instant startedAt,
	Instant dueAt,
	Instant completedAt,
	SprintStatus status,
	List<String> issueKeys,
	Instant createdAt,
	Long createdBy
) {
	public static SprintDetail from(Sprint sprint, List<String> issueKeys) {
		return SprintDetail.builder()
			.id(sprint.getId())
			.title(sprint.getTitle())
			.goal(sprint.getGoal())
			.startedAt(sprint.getStartedAt())
			.dueAt(sprint.getDueAt())
			.completedAt(sprint.getCompletedAt())
			.status(sprint.getStatus())
			.issueKeys(issueKeys)
			.createdAt(sprint.getCreatedAt())
			.createdBy(sprint.getCreatedBy())
			.build();
	}
}
