package com.tissue.api.issue.base.presentation.dto.request;

import java.util.List;

import com.tissue.api.issue.base.application.dto.ReorderOptionsCommand;

public record ReorderOptionsRequest(
	// @NotEmpty(message = "{valid.notempty}")
	List<Long> orderedIds
) {
	public ReorderOptionsCommand toCommand(String workspaceKey, Long issueTypeId, Long issueFieldId) {
		return ReorderOptionsCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.orderedIds(orderedIds)
			.build();
	}
}
