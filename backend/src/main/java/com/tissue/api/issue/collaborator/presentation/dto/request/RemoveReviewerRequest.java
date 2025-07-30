package com.tissue.api.issue.collaborator.presentation.dto.request;

import com.tissue.api.issue.collaborator.application.dto.RemoveReviewerCommand;

public record RemoveReviewerRequest(
	Long memberId
) {
	public RemoveReviewerCommand toCommand() {
		return new RemoveReviewerCommand(memberId);
	}
}
