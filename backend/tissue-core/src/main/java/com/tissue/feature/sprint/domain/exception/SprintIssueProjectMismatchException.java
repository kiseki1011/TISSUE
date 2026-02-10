package com.tissue.feature.sprint.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.shared.exception.base.BadRequestException;

public class SprintIssueProjectMismatchException extends BadRequestException {

    public SprintIssueProjectMismatchException(Issue issue, String sprintProjectKey) {
        super(SprintErrorCode.SPRINT_ISSUE_PROJECT_MISMATCH);
        addContext(ISSUE_KEY, issue.getKey());
        addContext("issueProjectKey", issue.getProjectKey());
        addContext("sprintProjectKey", sprintProjectKey);
    }
}
