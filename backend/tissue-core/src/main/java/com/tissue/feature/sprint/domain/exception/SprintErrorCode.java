package com.tissue.feature.sprint.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SprintErrorCode implements ErrorCode {
    SPRINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Sprint not found"),
    ACTIVE_SPRINT_ALREADY_EXISTS(HttpStatus.CONFLICT, "An active sprint already exists in the project"),
    INCOMPLETE_SPRINT_ISSUES_FOUND(HttpStatus.BAD_REQUEST, "Sprint contains incomplete issues"),
    SPRINT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "Sprint is already closed"),
    SPRINT_ISSUE_PROJECT_MISMATCH(HttpStatus.BAD_REQUEST, "Issue must belong to the same project as the sprint"),
    INVALID_SPRINT_PERIOD(HttpStatus.BAD_REQUEST, "Invalid sprint period"),
    INVALID_SPRINT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid sprint status transition"),
    SPRINT_NOT_CANCELLED(HttpStatus.BAD_REQUEST, "Sprint must be in CANCELLED status to be deleted");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
