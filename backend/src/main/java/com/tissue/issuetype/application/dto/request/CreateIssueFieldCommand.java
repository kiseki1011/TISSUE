package com.tissue.issuetype.application.dto.request;

import java.util.List;

import com.tissue.common.vo.Label;
import com.tissue.issuetype.domain.enums.FieldType;

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
