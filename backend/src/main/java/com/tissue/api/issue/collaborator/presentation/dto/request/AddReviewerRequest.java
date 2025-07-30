package com.tissue.api.issue.collaborator.presentation.dto.request;

import com.tissue.api.issue.collaborator.application.dto.AddReviewerCommand;

public record AddReviewerRequest(
	Long memberId
) {
	public AddReviewerCommand toCommand() {
		return new AddReviewerCommand(memberId);
	}
}
