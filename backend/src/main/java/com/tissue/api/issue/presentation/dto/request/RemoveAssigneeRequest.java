package com.tissue.api.issue.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record RemoveAssigneeRequest(
	@NotNull Long memberId
) {
}
