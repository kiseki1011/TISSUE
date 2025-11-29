package com.tissue.api.sprint.exception;

import java.util.List;

import com.tissue.api.common.exception.base.BadRequestException;

public class IncompleteSprintIssuesFoundException extends BadRequestException {

	public IncompleteSprintIssuesFoundException(List<String> issueKeys, Long sprintId, String projectKey) {
		super("Incomplete issues %s in sprint (id= '%d') were found. Pleases migrate these issues to another sprint."
			.formatted(issueKeys, sprintId));

		addContext("sprintId", sprintId);
		addContext("projectKey", projectKey);
	}
}
