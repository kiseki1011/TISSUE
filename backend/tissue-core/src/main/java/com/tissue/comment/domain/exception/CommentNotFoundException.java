package com.tissue.comment.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.COMMENT_ID;
import static com.tissue.common.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class CommentNotFoundException extends ResourceNotFoundException {

    public CommentNotFoundException(String issueKey, Long commentId) {
        super(CommentErrorCode.COMMENT_NOT_FOUND);
        addContext(ISSUE_KEY, issueKey);
        addContext(COMMENT_ID, commentId);
    }
}
