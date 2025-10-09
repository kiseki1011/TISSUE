package com.tissue.api.issue.presentation.dto.request;

import com.tissue.api.issue.application.dto.RemoveReviewerCommand;

public record RemoveReviewerRequest(
	Long memberId
) {
	public RemoveReviewerCommand toCommand() {
		return new RemoveReviewerCommand(memberId);
	}
}
