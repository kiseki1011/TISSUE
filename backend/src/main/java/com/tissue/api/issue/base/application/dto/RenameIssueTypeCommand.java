package com.tissue.api.issue.base.application.dto;

import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record RenameIssueTypeCommand(
	String workspaceKey,
	Long id,
	Label label
) {
}
