package com.tissue.comment.domain.exception;

import com.tissue.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND("Comment not found"),
    EDIT_NOT_ALLOWED("Must be the author to edit the comment"),
    NESTED_COMMENT_LIMIT_EXCEEDED("Comments can only be nested one level deep");

    private final String defaultMessage;
}
