package com.tissue.api.issuetype.application.dto;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record CreateIssueTypeCommand(
	String workspaceKey,
	Label label,
	String description,
	ColorType color,
	IssueHierarchy issueHierarchy,
	Long workflowId
) {
}
