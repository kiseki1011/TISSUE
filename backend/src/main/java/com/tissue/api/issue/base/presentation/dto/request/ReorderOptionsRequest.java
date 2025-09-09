package com.tissue.api.issue.base.presentation.dto.request;

import java.util.List;

import com.tissue.api.issue.base.application.dto.ReorderOptionsCommand;

public record ReorderOptionsRequest(
	// @NotEmpty(message = "{valid.notempty}")
	List<String> orderKeys
) {
	public ReorderOptionsCommand toCommand(String workspaceKey, String issueTypeKey, String issueFieldKey) {
		return ReorderOptionsCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeKey)
			.issueFieldKey(issueFieldKey)
			.orderKeys(orderKeys)
			.build();
	}
}
