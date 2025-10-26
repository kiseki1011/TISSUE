package com.tissue.api.issue.adapter.in.web.dto.request;

import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;

public record UpdateStoryPointRequest(
	Integer storyPoint
) {
	public UpdateStoryPointCommand toCommand(String workspaceKey, String issueKey) {
		return new UpdateStoryPointCommand(workspaceKey, issueKey, storyPoint);
	}
}
