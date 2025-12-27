package com.tissue.issuetype.application.dto.request;

import java.util.List;

import lombok.Builder;

@Builder
public record ReorderOptionsCommand(
	String workspaceKey,
	String projectKey,
	Long issueTypeId,
	Long issueFieldId,
	List<Long> targetOrderedIds
) {
}
