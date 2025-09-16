package com.tissue.api.issue.base.application.dto;

import lombok.Builder;

@Builder
public record RenameOptionCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	Long optionId,
	String label
) {
}
