package com.tissue.api.issue.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record PerformTransitionRequest(
	@NotNull Long transitionId
) {
}
