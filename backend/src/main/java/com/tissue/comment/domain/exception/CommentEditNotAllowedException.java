package com.tissue.comment.domain.exception;

import static com.tissue.comment.domain.exception.CommentErrorCode.EDIT_NOT_ALLOWED;
import static com.tissue.common.exception.ErrorContextKeys.ACTOR_MEMBER_ID;
import static com.tissue.common.exception.ErrorContextKeys.COMMENT_ID;
import static com.tissue.common.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.comment.domain.Comment;
import com.tissue.common.exception.base.ForbiddenException;

public class CommentEditNotAllowedException extends ForbiddenException {

    public CommentEditNotAllowedException(Comment comment, Long actorMemberId) {
        super(EDIT_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, comment.getIssue().getWorkspaceKey());
        addContext(PROJECT_KEY, comment.getIssue().getProjectKey());
        addContext(ISSUE_KEY, comment.getIssue().getKey());
        addContext(COMMENT_ID, comment.getId());
        addContext(ACTOR_MEMBER_ID, actorMemberId);
    }
}
