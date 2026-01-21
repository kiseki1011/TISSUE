package com.tissue.comment.domain.exception;

import static com.tissue.global.exception.ContextKeys.COMMENT_ID;
import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class CommentNotFoundException extends ResourceNotFoundException {

    public CommentNotFoundException(Long commentId, String issueKey) {
        super(CommentErrorCode.COMMENT_NOT_FOUND);
        addContext(ISSUE_KEY, issueKey);
        addContext(COMMENT_ID, commentId);
    }
}
