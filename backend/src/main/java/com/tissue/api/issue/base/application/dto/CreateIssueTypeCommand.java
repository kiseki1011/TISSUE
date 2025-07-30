package com.tissue.api.issue.base.application.dto;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.domain.enums.HierarchyLevel;

import lombok.Builder;

@Builder
public record CreateIssueTypeCommand(
	String workspaceCode,
	String label,
	ColorType color,
	HierarchyLevel hierarchyLevel,
	String workflowKey
) {
}
