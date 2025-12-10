package com.tissue.api.issuetype.application.dto.request;

import java.util.List;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.domain.enums.FieldType;

import lombok.Builder;

@Builder
public record CreateIssueFieldCommand(
	String workspaceKey,
	String projectKey,
	Long issueTypeId,
	Label label,
	String description,
	FieldType fieldType,
	Boolean required,
	List<Label> initialOptions
) {
}
