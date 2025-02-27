package com.tissue.api.sprint.presentation.condition;

import java.util.List;

import com.tissue.api.sprint.domain.enums.SprintStatus;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SprintSearchCondition(
	List<SprintStatus> statuses,
	@Size(min = 3, message = "{valid.size.keyword}")
	String keyword
) {
	public SprintSearchCondition {
		if (statuses == null || statuses.isEmpty()) {
			statuses = List.of(SprintStatus.PLANNING, SprintStatus.ACTIVE);
		}
		if (keyword != null && keyword.trim().isEmpty()) {
			keyword = null;
		}
	}

	public SprintSearchCondition() {
		this(
			List.of(SprintStatus.PLANNING, SprintStatus.ACTIVE),
			null
		);
	}
}
