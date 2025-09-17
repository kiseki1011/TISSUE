package com.tissue.api.issue.base.application.dto;

import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record RenameIssueFieldCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	Label label
) {
}
