package com.tissue.api.project.domain.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;

public class ProjectMemberAlreadyExistsException extends ResourceConflictException {

	public static final String MESSAGE = "Project member already exists in project(workspace key '%s', project key '%s') for member id '%d'.";

	public ProjectMemberAlreadyExistsException(String workspaceKey, String projectKey, Long memberId) {
		super(MESSAGE.formatted(workspaceKey, projectKey, memberId));
		addContext("workspaceKey", workspaceKey);
		addContext("projectKey", projectKey);
		addContext("memberId", memberId);
	}
}
