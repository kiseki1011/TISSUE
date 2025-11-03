package com.tissue.api.issuetype.application.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ReorderOptionsCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	List<Long> targetOrderedIds
) {
}
