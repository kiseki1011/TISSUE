package com.tissue.api.issue.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddReviewerRequest(
	@NotNull Long memberId
) {
}
