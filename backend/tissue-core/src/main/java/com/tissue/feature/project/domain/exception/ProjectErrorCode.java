package com.tissue.feature.project.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements ErrorCode {
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Project not found"),
    PROJECT_ARCHIVED(HttpStatus.BAD_REQUEST, "Cannot modify archived project and its resources"),
    DUPLICATE_PROJECT_KEY(HttpStatus.CONFLICT, "Project key already exists"),
    RESERVED_PROJECT_KEY(HttpStatus.BAD_REQUEST, "Cannot use reserved project key"),
    INVALID_PROJECT_KEY_FORMAT(
            HttpStatus.BAD_REQUEST, "Project key must be 2-10 uppercase letters, optionally followed by numbers"),

    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Project member not found"),
    SELF_KICK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Cannot kick yourself from project"),

    PROJECT_JOIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "Requires permission to join private project"),
    PROJECT_MANAGER_MODIFICATION_NOT_ALLOWED(
            HttpStatus.FORBIDDEN, "Insufficient permission to change role or kick a project manager"),
    PROJECT_MANAGER_REQUIRED(HttpStatus.FORBIDDEN, "Requires project manager role"),

    PROJECT_NOT_SOFT_DELETED(HttpStatus.CONFLICT, "Project must be soft-deleted before it can be permanently deleted"),
    HARD_DELETE_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST, "Confirmation key does not match the target project key");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
