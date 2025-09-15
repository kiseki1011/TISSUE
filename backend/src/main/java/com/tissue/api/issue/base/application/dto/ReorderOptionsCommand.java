package com.tissue.api.issue.base.application.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ReorderOptionsCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	List<Long> orderedIds
) {
}
