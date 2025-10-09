package com.tissue.api.issuetype.application.dto;

import com.tissue.api.issue.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record AddOptionCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	Label label
) {
}
