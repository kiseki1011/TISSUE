package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.CURRENT_HIERARCHY;
import static com.tissue.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.exception.ErrorContextKeys.STORY_POINT_ALLOWED_HIERARCHIES;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueHierarchy;

public class StoryPointNotAllowedException extends BadRequestException {

    public StoryPointNotAllowedException(String workspaceKey, String issueKey, IssueHierarchy currentHierarchy) {
        super(IssueErrorCode.STORY_POINT_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
        addContext(CURRENT_HIERARCHY, currentHierarchy);
        addContext(STORY_POINT_ALLOWED_HIERARCHIES, IssueHierarchy.getStoryPointModifiable());
    }
}
