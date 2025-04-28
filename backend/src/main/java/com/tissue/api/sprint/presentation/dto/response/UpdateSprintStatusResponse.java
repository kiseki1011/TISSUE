package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.enums.SprintStatus;

import lombok.Builder;

@Builder
public record UpdateSprintStatusResponse(
	Long id,
	String sprintKey,
	SprintStatus status,
	LocalDateTime updatedAt,
	Long updatedBy
) {
	public static UpdateSprintStatusResponse from(Sprint sprint) {
		return UpdateSprintStatusResponse.builder()
			.id(sprint.getId())
			.sprintKey(sprint.getSprintKey())
			.status(sprint.getStatus())
			.updatedAt(sprint.getLastModifiedDate())
			.updatedBy(sprint.getLastModifiedBy())
			.build();
	}
}
