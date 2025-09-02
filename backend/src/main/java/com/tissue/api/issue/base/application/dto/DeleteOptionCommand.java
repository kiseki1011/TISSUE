package com.tissue.api.issue.base.application.dto;

public record DeleteOptionCommand(
	String workspaceKey,
	String issueTypeKey,
	String issueFieldKey,
	String optionKey
) {
}
