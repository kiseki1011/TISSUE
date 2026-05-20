package com.tissue.feature.comment.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Comment not found"),
    COMMENT_EDIT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "Must be the author to edit the comment"),
    NESTED_COMMENT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "Comments can only be nested one level deep"),
    COMMENT_PARENT_ISSUE_MISMATCH(HttpStatus.BAD_REQUEST, "Parent comment must belong to the same issue");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
