package com.tissue.api.review.presentation.dto.request;

import com.tissue.api.review.service.dto.AddReviewerCommand;

public record AddReviewerRequest(
	Long memberId
) {
	public AddReviewerCommand toCommand() {
		return new AddReviewerCommand(memberId);
	}
}
