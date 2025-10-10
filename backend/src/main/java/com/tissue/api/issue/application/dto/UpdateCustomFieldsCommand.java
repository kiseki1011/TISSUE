package com.tissue.api.issue.application.dto;

import java.util.Map;

public record UpdateCustomFieldsCommand(
	String workspaceKey,
	String issueKey,
	Map<Long, Object> customFields
) {
}
