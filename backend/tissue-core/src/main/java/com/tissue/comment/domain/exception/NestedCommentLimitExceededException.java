package com.tissue.comment.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.PARENT_COMMENT_ID;

import com.tissue.common.exception.base.BadRequestException;

public class NestedCommentLimitExceededException extends BadRequestException {

    public NestedCommentLimitExceededException(Long parentId) {
        super(CommentErrorCode.NESTED_COMMENT_LIMIT_EXCEEDED);
        addContext(PARENT_COMMENT_ID, parentId);
    }
}
