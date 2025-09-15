package com.tissue.api.issue.base.application.dto;

import lombok.Builder;

@Builder
public record AddOptionCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	String label
) {
}
