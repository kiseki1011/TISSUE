package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.sprint.domain.Sprint;

import lombok.Builder;

@Builder
public record AddSprintIssuesResponse(
	Long sprintId,
	String sprintKey,
	List<String> addedIssueKeys,
	LocalDateTime addedAt
) {
	public static AddSprintIssuesResponse of(
		Sprint sprint,
		List<String> addedIssueKeys
	) {
		return AddSprintIssuesResponse.builder()
			.sprintId(sprint.getId())
			.sprintKey(sprint.getSprintKey())
			.addedIssueKeys(addedIssueKeys)
			.addedAt(LocalDateTime.now())
			.build();
	}
}
