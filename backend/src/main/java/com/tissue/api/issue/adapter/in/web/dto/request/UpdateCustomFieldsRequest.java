package com.tissue.api.issue.adapter.in.web.dto.request;

import java.util.Map;

import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateCustomFieldsRequest(
	@NotEmpty @NotNull Map<Long, Object> customFields
) {
	public UpdateCustomFieldsCommand toCommand(String workspaceKey, String issueKey) {
		return new UpdateCustomFieldsCommand(
			workspaceKey,
			issueKey,
			customFields
		);
	}
}
