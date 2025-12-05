package com.tissue.api.issuetype.application.dto.request;

import com.tissue.api.common.vo.Label;

import lombok.Builder;

@Builder
public record AddOptionCommand(
	String workspaceKey,
	String projectKey,
	Long issueTypeId,
	Long issueFieldId,
	Label label
) {
}
