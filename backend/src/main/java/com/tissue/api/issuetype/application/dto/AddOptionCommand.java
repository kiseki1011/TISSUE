package com.tissue.api.issuetype.application.dto;

import com.tissue.api.common.vo.Label;

import lombok.Builder;

@Builder
public record AddOptionCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	Label label
) {
}
