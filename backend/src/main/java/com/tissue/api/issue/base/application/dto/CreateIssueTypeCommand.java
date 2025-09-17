package com.tissue.api.issue.base.application.dto;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.domain.enums.HierarchyLevel;
import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record CreateIssueTypeCommand(
	String workspaceKey,
	Label label,
	String description,
	ColorType color,
	HierarchyLevel hierarchyLevel,
	String workflowKey
) {
}
