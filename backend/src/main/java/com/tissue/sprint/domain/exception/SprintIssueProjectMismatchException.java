package com.tissue.sprint.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class SprintIssueProjectMismatchException extends BadRequestException {

	public static final String MESSAGE = "Issue '%s' belongs to project '%s' but sprint belongs to '%s'. Cross project sprints are not allowed.";

	public SprintIssueProjectMismatchException(String issueKey, String issueProjectKey, String sprintProjectKey) {
		super(MESSAGE.formatted(issueKey, issueProjectKey, sprintProjectKey));

		addContext("issueKey", issueKey);
		addContext("issueProjectKey", issueProjectKey);
		addContext("sprintProjectKey", sprintProjectKey);
	}
}
