package com.tissue.project.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class ProjectArchivedException extends BadRequestException {

	public ProjectArchivedException(String projectKey, String workspaceKey) {
		super("Project '%s' is archived. Cannot modify.".formatted(projectKey));
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}
}
