package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.ShortText;
import com.tissue.api.issue.base.application.dto.AssignParentIssueCommand;

import jakarta.validation.constraints.NotBlank;

public record AssignParentIssueRequest(
	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String parentIssueKey
) {
	public AssignParentIssueCommand toCommand(String workspaceCode, String issueKey) {
		return new AssignParentIssueCommand(workspaceCode, issueKey, parentIssueKey);
	}
}
