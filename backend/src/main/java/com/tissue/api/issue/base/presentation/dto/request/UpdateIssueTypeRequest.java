package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.application.dto.UpdateIssueTypeCommand;

public record UpdateIssueTypeRequest(
	String label,
	ColorType color
) {
	public UpdateIssueTypeCommand toCommand(String workspaceKey, String issueTypeKey) {
		return UpdateIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeKey)
			.label(label)
			.color(color)
			.build();
	}
}
