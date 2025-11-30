package com.tissue.api.project.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class ProjectMemberNotFoundException extends ResourceNotFoundException {

	private static final String MESSAGE = "Cannot find project member with workspace key '%s', project key '%s', member id '%d'.";

	public ProjectMemberNotFoundException(String workspaceKey, String projectKey, Long memberId) {
		super(MESSAGE.formatted(workspaceKey, projectKey, memberId));
		addContext("workspaceKey", workspaceKey);
		addContext("projectKey", projectKey);
		addContext("memberId", memberId);
	}
}
