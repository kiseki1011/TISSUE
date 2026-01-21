package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.RELATION_TYPE;
import static com.tissue.global.exception.ContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.TARGET_ISSUE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueRelationType;
import java.util.List;

public class RelationCycleDetectedException extends BadRequestException {

    public RelationCycleDetectedException(
            String sourceIssueKey, String targetIssueKey, IssueRelationType relationType, List<String> path) {
        super(IssueErrorCode.RELATION_CIRCULAR_DEPENDENCY);
        addContext(SOURCE_ISSUE_KEY, sourceIssueKey);
        addContext(TARGET_ISSUE_KEY, targetIssueKey);
        addContext(RELATION_TYPE, relationType.name());
        addContext("detectedCyclePath", path);
    }
}
