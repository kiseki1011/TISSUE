package com.tissue.issuetype.application.dto.request;

import com.tissue.common.vo.Label;

import lombok.Builder;

@Builder
public record RenameIssueTypeCommand(
	String workspaceKey,
	String projectKey,
	Long id,
	Label label
) {
}
