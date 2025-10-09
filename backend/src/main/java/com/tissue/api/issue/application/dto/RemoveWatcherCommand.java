package com.tissue.api.issue.application.dto;

public record RemoveWatcherCommand(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
}
