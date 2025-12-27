package com.tissue.sprint.domain.exception;

import com.tissue.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SprintErrorCode implements ErrorCode {
    SPRINT_NOT_FOUND("Sprint not found"),
    ACTIVE_SPRINT_ALREADY_EXISTS("An active sprint already exists in the project"),
    INCOMPLETE_SPRINT_ISSUES_FOUND("Sprint contains incomplete issues"),
    SPRINT_ALREADY_CLOSED("Sprint is already closed"),
    SPRINT_ISSUE_PROJECT_MISMATCH("Issue must belong to the same project as the sprint"),
    INVALID_SPRINT_PERIOD("Invalid sprint period"),
    INVALID_SPRINT_STATUS_TRANSITION("Invalid sprint status transition");

    private final String defaultMessage;
}
