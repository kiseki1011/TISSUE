package com.tissue.api.issue.base.application.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ReorderOptionsCommand(
	String workspaceKey,
	String issueTypeKey,
	String issueFieldKey,
	List<String> orderKeys
) {
}
