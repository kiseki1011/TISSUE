package com.tissue.api.issue.application.dto;

public record AddWatcherCommand(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
}
