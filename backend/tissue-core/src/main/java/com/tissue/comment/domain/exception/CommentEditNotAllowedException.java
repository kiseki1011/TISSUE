package com.tissue.comment.domain.exception;

import static com.tissue.comment.domain.exception.CommentErrorCode.EDIT_NOT_ALLOWED;
import static com.tissue.exception.ErrorContextKeys.ACTOR_MEMBER_ID;
import static com.tissue.exception.ErrorContextKeys.COMMENT_ID;
import static com.tissue.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.exception.base.ForbiddenException;

public class CommentEditNotAllowedException extends ForbiddenException {

    public CommentEditNotAllowedException(String issueKey, Long commentId, Long actorMemberId) {
        super(EDIT_NOT_ALLOWED);
        addContext(ISSUE_KEY, issueKey);
        addContext(COMMENT_ID, commentId);
        addContext(ACTOR_MEMBER_ID, actorMemberId);
    }
}
