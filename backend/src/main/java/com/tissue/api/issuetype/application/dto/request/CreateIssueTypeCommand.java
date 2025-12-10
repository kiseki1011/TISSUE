package com.tissue.api.issuetype.application.dto.request;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.enums.IssueHierarchy;

import lombok.Builder;

@Builder
public record CreateIssueTypeCommand(
	String workspaceKey,
	String projectKey,
	Label label,
	String description,
	ColorType color,
	IssueHierarchy issueHierarchy,
	Long workflowId
) {
}
