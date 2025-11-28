package com.tissue.api.project.domain.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;

public class DuplicateProjectKeyException extends ResourceConflictException {

	public static final String MESSAGE = "Project key '%s' is duplicate in workspace '%s'.";

	public DuplicateProjectKeyException(String workspaceKey, String projectKey) {
		super(MESSAGE.formatted(projectKey, workspaceKey));
		addContext("workspaceKey", workspaceKey);
		addContext("projectKey", projectKey);
	}
}
