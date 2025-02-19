package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.sprint.domain.Sprint;

import lombok.Builder;

@Builder
public record UpdateSprintDateResponse(
	Long id,
	String sprintKey,
	LocalDate startDate,
	LocalDate endDate,
	LocalDateTime updatedAt,
	Long updatedBy
) {
	public static UpdateSprintDateResponse from(Sprint sprint) {
		return UpdateSprintDateResponse.builder()
			.id(sprint.getId())
			.sprintKey(sprint.getSprintKey())
			.startDate(sprint.getStartDate())
			.endDate(sprint.getEndDate())
			.updatedAt(sprint.getLastModifiedDate())
			.updatedBy(sprint.getLastModifiedByWorkspaceMember())
			.build();
	}
}
