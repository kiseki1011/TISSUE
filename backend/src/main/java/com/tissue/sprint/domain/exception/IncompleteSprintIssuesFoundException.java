package com.tissue.sprint.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.SPRINT_ID;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.sprint.domain.Sprint;
import java.util.List;

public class IncompleteSprintIssuesFoundException extends BadRequestException {

    public IncompleteSprintIssuesFoundException(List<String> issueKeys, Sprint sprint) {
        super(SprintErrorCode.INCOMPLETE_SPRINT_ISSUES_FOUND);
        addContext("incompleteIssueKeys", issueKeys);
        addContext(SPRINT_ID, sprint.getId());
        addContext(PROJECT_KEY, sprint.getProjectKey());
    }
}
