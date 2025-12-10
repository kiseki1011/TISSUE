package com.tissue.api.issuetype.application.dto.request;

import com.tissue.api.common.vo.Label;

import lombok.Builder;

@Builder
public record RenameIssueTypeCommand(
	String workspaceKey,
	String projectKey,
	Long id,
	Label label
) {
}
