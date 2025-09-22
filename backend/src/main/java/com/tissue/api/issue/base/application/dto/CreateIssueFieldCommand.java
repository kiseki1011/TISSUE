package com.tissue.api.issue.base.application.dto;

import java.util.List;

import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record CreateIssueFieldCommand(
	String workspaceKey,
	Long issueTypeId,
	Label label,
	String description,
	FieldType fieldType,
	Boolean required,
	List<Label> initialOptions
) {
}
