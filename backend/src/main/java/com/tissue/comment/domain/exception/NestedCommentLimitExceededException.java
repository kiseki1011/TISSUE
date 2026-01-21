package com.tissue.comment.domain.exception;

import static com.tissue.global.exception.ContextKeys.PARENT_COMMENT_ID;

import com.tissue.global.exception.base.BadRequestException;

public class NestedCommentLimitExceededException extends BadRequestException {

    public NestedCommentLimitExceededException(Long parentId) {
        super(CommentErrorCode.NESTED_COMMENT_LIMIT_EXCEEDED);
        addContext(PARENT_COMMENT_ID, parentId);
    }
}
