package com.tissue.api.issuetype.application.dto;

import lombok.Builder;

@Builder
public record DeleteOptionCommand(
	String workspaceKey,
	String projectKey,
	Long issueTypeId,
	Long issueFieldId,
	Long optionId
) {
}
