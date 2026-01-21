package com.tissue.sprint.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.issue.domain.Issue;

public class SprintIssueProjectMismatchException extends BadRequestException {

    public SprintIssueProjectMismatchException(Issue issue, String sprintProjectKey) {
        super(SprintErrorCode.SPRINT_ISSUE_PROJECT_MISMATCH);
        addContext(ISSUE_KEY, issue.getKey());
        addContext("issueProjectKey", issue.getProjectKey());
        addContext("sprintProjectKey", sprintProjectKey);
    }
}
