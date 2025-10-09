package com.tissue.api.issuetype.application.dto;

import com.tissue.api.issue.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record RenameOptionCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	Long optionId,
	Label label
) {
}
