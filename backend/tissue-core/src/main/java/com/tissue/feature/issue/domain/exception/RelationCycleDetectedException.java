package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.RELATION_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.TARGET_ISSUE_KEY;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.exception.base.BadRequestException;
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
