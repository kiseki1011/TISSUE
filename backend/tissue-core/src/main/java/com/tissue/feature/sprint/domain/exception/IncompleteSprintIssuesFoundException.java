package com.tissue.feature.sprint.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.SPRINT_ID;

import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;

public class IncompleteSprintIssuesFoundException extends BadRequestException {

    public IncompleteSprintIssuesFoundException(List<String> issueKeys, String projectKey, Long sprintId) {
        super(SprintErrorCode.INCOMPLETE_SPRINT_ISSUES_FOUND);
        addContext("incompleteIssueKeys", issueKeys);
        addContext(PROJECT_KEY, projectKey);
        addContext(SPRINT_ID, sprintId);
    }
}
