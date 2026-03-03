package com.tissue.feature.comment.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PARENT_COMMENT_ID;

import com.tissue.shared.exception.base.BadRequestException;

public class NestedCommentLimitExceededException extends BadRequestException {

    public NestedCommentLimitExceededException(Long parentId) {
        super(CommentErrorCode.NESTED_COMMENT_LIMIT_EXCEEDED);
        addContext(PARENT_COMMENT_ID, parentId);
    }
}
