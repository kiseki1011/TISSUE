package com.tissue.api.issue.collaborator.application.dto;

public record AddWatcherCommand(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
}
