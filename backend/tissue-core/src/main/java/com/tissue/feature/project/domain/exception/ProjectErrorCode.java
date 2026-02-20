package com.tissue.feature.project.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements ErrorCode {
    PROJECT_NOT_FOUND("Project not found"),
    PROJECT_ARCHIVED("Cannot modify archived project and its resources"),
    DUPLICATE_PROJECT_KEY("Project key is duplicate in workspace"),
    RESERVED_PROJECT_KEY("Cannot use reserved project key"),
    INVALID_PROJECT_KEY_FORMAT("Project key must be 2-10 uppercase letters, optionally followed by numbers"),

    PROJECT_MEMBER_NOT_FOUND("Project member not found"),
    SELF_KICK_NOT_ALLOWED("Cannot kick yourself from project"),

    PROJECT_EDIT_PERMISSION_REQUIRED("Insufficient permission to edit. Contact workspace admin."),
    PROJECT_JOIN_NOT_ALLOWED("Requires permission to join private project"),
    PROJECT_MANAGER_MODIFICATION_NOT_ALLOWED("Insufficient permission to change role or kick a project manager"),
    PROJECT_MANAGER_REQUIRED("Requires project manager role");

    private final String defaultMessage;
}
