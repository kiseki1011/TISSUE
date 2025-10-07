package com.tissue.api.issue.collaborator.application.dto;

public record RemoveWatcherCommand(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
}
