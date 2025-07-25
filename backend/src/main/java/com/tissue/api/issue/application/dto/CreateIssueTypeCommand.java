package com.tissue.api.issue.application.dto;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.domain.model.enums.HierarchyLevel;

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
