package com.tissue.feature.comment.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND("Comment not found"),
    COMMENT_EDIT_NOT_ALLOWED("Must be the author to edit the comment"),
    NESTED_COMMENT_LIMIT_EXCEEDED("Comments can only be nested one level deep"),
    COMMENT_PARENT_ISSUE_MISMATCH("Parent comment must belong to the same issue");

    private final String defaultMessage;
}
