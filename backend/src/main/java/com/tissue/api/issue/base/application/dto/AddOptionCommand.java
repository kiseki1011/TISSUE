package com.tissue.api.issue.base.application.dto;

import lombok.Builder;

@Builder
public record AddOptionCommand(
	String workspaceKey,
	String issueTypeKey,
	String issueFieldKey,
	String label
) {
}
