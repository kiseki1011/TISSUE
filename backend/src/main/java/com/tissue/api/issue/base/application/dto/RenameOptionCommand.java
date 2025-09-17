package com.tissue.api.issue.base.application.dto;

import com.tissue.api.issue.base.domain.model.vo.Label;

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
