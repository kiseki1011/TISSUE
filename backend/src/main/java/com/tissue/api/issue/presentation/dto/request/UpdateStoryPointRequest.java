package com.tissue.api.issue.presentation.dto.request;

import com.tissue.api.issue.application.dto.UpdateStoryPointCommand;

public record UpdateStoryPointRequest(
	Integer storyPoint
) {
	public UpdateStoryPointCommand toCommand(String workspaceKey, String issueKey) {
		return new UpdateStoryPointCommand(workspaceKey, issueKey, storyPoint);
	}
}
