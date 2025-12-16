package com.tissue.sprint.application.dto.request;

import java.util.List;

public record RemoveSprintIssuesCommand(
	String workspaceKey,
	String projectKey,
	Long sprintId,
	List<String> issueKeys
) {
}
