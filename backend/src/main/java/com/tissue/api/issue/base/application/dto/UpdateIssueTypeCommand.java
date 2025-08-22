package com.tissue.api.issue.base.application.dto;

import com.tissue.api.common.enums.ColorType;

import lombok.Builder;

@Builder
public record UpdateIssueTypeCommand(
	String workspaceKey,
	String issueTypeKey,
	String label,
	ColorType color
) {
}
