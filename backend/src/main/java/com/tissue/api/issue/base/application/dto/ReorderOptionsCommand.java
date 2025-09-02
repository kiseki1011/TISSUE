package com.tissue.api.issue.base.application.dto;

import java.util.List;

public record ReorderOptionsCommand(
	String workspaceKey,
	String issueTypeKey,
	String issueFieldKey,
	List<String> orderKeys
) {
}
