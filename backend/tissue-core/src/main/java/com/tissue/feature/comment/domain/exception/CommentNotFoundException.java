package com.tissue.feature.comment.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.COMMENT_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class CommentNotFoundException extends ResourceNotFoundException {

    public CommentNotFoundException(String issueKey, Long commentId) {
        super(CommentErrorCode.COMMENT_NOT_FOUND);
        addContext(ISSUE_KEY, issueKey);
        addContext(COMMENT_ID, commentId);
    }
}
