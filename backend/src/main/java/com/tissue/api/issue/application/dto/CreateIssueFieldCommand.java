package com.tissue.api.issue.application.dto;

import java.util.List;

import com.tissue.api.issue.domain.model.enums.FieldType;

import lombok.Builder;

@Builder
public record CreateIssueFieldCommand(
	String workspaceCode,
	String issueTypeKey,
	String label,
	String description,
	FieldType fieldType,
	Boolean required,
	List<String> allowedOptions
) {
}
