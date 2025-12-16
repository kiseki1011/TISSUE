package com.tissue.issuetype.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Label;
import com.tissue.issue.domain.enums.IssueHierarchy;

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
