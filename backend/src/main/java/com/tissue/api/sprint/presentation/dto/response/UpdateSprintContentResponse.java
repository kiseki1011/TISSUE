package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.sprint.domain.Sprint;

import lombok.Builder;

@Builder
public record UpdateSprintContentResponse(
	Long id,
	String sprintKey,
	String title,
	String goal,
	LocalDateTime updatedAt,
	Long updatedBy
) {
	public static UpdateSprintContentResponse from(Sprint sprint) {
		return UpdateSprintContentResponse.builder()
			.id(sprint.getId())
			.sprintKey(sprint.getSprintKey())
			.title(sprint.getTitle())
			.goal(sprint.getGoal())
			.updatedAt(sprint.getLastModifiedDate())
			.updatedBy(sprint.getLastModifiedByWorkspaceMember())
			.build();
	}
}
