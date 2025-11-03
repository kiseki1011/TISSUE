package com.tissue.api.issuetype.application.dto;

import com.tissue.api.common.vo.Label;

import lombok.Builder;

@Builder
public record RenameIssueTypeCommand(
	String workspaceKey,
	Long id,
	Label label
) {
}
