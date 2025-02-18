package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.enums.SprintStatus;

import lombok.Builder;

@Builder
public record SprintDetailResponse(
	Long id,
	String sprintKey,
	String title,
	String goal,
	LocalDate startDate,
	LocalDate endDate,
	SprintStatus status,
	List<String> issueKeys,
	LocalDateTime createdAt,
	Long createdBy
) {
	public static SprintDetailResponse from(Sprint sprint) {
		return SprintDetailResponse.builder()
			.id(sprint.getId())
			.sprintKey(sprint.getSprintKey())
			.title(sprint.getTitle())
			.goal(sprint.getGoal())
			.startDate(sprint.getStartDate())
			.endDate(sprint.getEndDate())
			.status(sprint.getStatus())
			.issueKeys(sprint.getSprintIssues().stream()
				.map(si -> si.getIssue().getIssueKey())
				.sorted()
				.toList())
			.createdAt(sprint.getCreatedDate())
			.createdBy(sprint.getCreatedByWorkspaceMember())
			.build();
	}
}
