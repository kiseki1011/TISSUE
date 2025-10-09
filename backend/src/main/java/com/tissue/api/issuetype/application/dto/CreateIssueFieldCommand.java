package com.tissue.api.issuetype.application.dto;

import java.util.List;

import com.tissue.api.issuetype.domain.enums.FieldType;
import com.tissue.api.issue.domain.model.vo.Label;

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
