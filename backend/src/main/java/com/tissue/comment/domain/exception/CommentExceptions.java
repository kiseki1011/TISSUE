package com.tissue.comment.domain.exception;

import static com.tissue.comment.domain.exception.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.tissue.comment.domain.exception.CommentErrorCode.NESTED_COMMENT_LIMIT_EXCEEDED;
import static com.tissue.comment.domain.exception.CommentErrorCode.NOT_COMMENT_AUTHOR;
import static com.tissue.global.exception.ContextKeys.COMMENT_ID;
import static com.tissue.global.exception.ContextKeys.MEMBER_ID;
import static com.tissue.global.exception.ContextKeys.PARENT_COMMENT_ID;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.global.exception.base.ResourceNotFoundException;

public class CommentExceptions {

    private CommentExceptions() {}

    public static ResourceNotFoundException notFound(Long commentId) {
        return new ResourceNotFoundException(COMMENT_NOT_FOUND).addContext(COMMENT_ID, commentId);
    }

    public static ForbiddenException notAuthor(Long commentId, Long memberId) {
        return new ForbiddenException(NOT_COMMENT_AUTHOR)
                .addContext(COMMENT_ID, commentId)
                .addContext(MEMBER_ID, memberId);
    }

    public static BadRequestException nestedLimitExceeded(Long parentId) {
        return new BadRequestException(NESTED_COMMENT_LIMIT_EXCEEDED).addContext(PARENT_COMMENT_ID, parentId);
    }
}
