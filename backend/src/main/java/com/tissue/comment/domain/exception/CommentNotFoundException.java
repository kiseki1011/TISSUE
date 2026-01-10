package com.tissue.comment.domain.exception;

import static com.tissue.global.exception.ContextKeys.COMMENT_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class CommentNotFoundException extends ResourceNotFoundException {

    public CommentNotFoundException(Long commentId) {
        super(CommentErrorCode.COMMENT_NOT_FOUND);
        addContext(COMMENT_ID, commentId);
    }
}
