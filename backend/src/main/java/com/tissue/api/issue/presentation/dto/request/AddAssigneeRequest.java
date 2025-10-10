package com.tissue.api.issue.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddAssigneeRequest(
	@NotNull Long memberId
) {
}
