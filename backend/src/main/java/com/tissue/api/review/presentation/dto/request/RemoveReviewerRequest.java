package com.tissue.api.review.presentation.dto.request;

import com.tissue.api.review.service.dto.RemoveReviewerCommand;

public record RemoveReviewerRequest(
	Long memberId
) {
	public RemoveReviewerCommand toCommand() {
		return new RemoveReviewerCommand(memberId);
	}
}
