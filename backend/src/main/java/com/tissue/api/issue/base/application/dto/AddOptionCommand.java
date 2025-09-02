package com.tissue.api.issue.base.application.dto;

public record AddOptionCommand(
	String workspaceKey,
	String issueTypeKey,
	String issueFieldKey,
	String label
) {
}
