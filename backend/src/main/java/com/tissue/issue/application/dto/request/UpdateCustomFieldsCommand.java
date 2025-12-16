package com.tissue.issue.application.dto.request;

import java.util.Map;

public record UpdateCustomFieldsCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	Map<Long, Object> customFields,
	Long actorMemberId
) {
}
