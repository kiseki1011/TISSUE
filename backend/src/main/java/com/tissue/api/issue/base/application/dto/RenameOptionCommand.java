package com.tissue.api.issue.base.application.dto;

import lombok.Builder;

@Builder
public record RenameOptionCommand(
	String workspaceKey,
	String issueTypeKey,
	String issueFieldKey,
	String optionKey,
	String newLabel
) {
}
