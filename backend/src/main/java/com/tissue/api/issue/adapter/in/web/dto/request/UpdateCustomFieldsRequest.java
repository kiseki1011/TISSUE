package com.tissue.api.issue.adapter.in.web.dto.request;

import java.util.Map;

import org.springframework.lang.Nullable;

import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;

public record UpdateCustomFieldsRequest(
	@Nullable Map<Long, Object> customFields
) {
	public UpdateCustomFieldsCommand toCommand(String workspaceKey, String issueKey) {
		return new UpdateCustomFieldsCommand(workspaceKey, issueKey, customFields == null ? Map.of() : customFields);
	}
}
