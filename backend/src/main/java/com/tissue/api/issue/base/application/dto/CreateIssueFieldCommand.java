package com.tissue.api.issue.base.application.dto;

import java.util.List;

import com.tissue.api.issue.base.domain.enums.FieldType;

import lombok.Builder;

@Builder
public record CreateIssueFieldCommand(
	String workspaceKey,
	Long issueTypeId,
	String label,
	String description,
	FieldType fieldType,
	Boolean required,
	List<String> initialOptions
) {
}
