package com.tissue.feature.sprint.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.SPRINT_ID;

import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;

public class IncompleteSprintIssuesFoundException extends BadRequestException {

    public IncompleteSprintIssuesFoundException(List<String> issueKeys, Sprint sprint) {
        super(SprintErrorCode.INCOMPLETE_SPRINT_ISSUES_FOUND);
        addContext("incompleteIssueKeys", issueKeys);
        addContext(SPRINT_ID, sprint.getId());
        addContext(PROJECT_KEY, sprint.getProjectKey());
    }
}
