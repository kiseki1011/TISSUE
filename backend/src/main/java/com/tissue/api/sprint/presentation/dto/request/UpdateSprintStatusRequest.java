package com.tissue.api.sprint.presentation.dto.request;

import com.tissue.api.sprint.domain.enums.SprintStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateSprintStatusRequest(

	@NotNull(message = "{valid.notnull}")
	SprintStatus newStatus
) {
}
