package com.tissue.issuetype.application.dto.request;

import com.tissue.common.vo.Label;

import lombok.Builder;

@Builder
public record RenameOptionCommand(
	String workspaceKey,
	String projectKey,
	Long issueTypeId,
	Long issueFieldId,
	Long optionId,
	Label label
) {
}
