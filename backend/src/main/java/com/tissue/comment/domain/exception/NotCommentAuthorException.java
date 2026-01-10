package com.tissue.comment.domain.exception;

import static com.tissue.global.exception.ContextKeys.COMMENT_ID;
import static com.tissue.global.exception.ContextKeys.MEMBER_ID;

import com.tissue.global.exception.base.ForbiddenException;

public class NotCommentAuthorException extends ForbiddenException {

    public NotCommentAuthorException(Long commentId, Long memberId) {
        super(CommentErrorCode.NOT_COMMENT_AUTHOR);
        addContext(COMMENT_ID, commentId);
        addContext(MEMBER_ID, memberId);
    }
}
