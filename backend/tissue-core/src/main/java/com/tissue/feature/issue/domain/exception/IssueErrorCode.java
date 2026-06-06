package com.tissue.feature.issue.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IssueErrorCode implements ErrorCode {
    ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "Issue not found"),
    INVALID_PARENT_HIERARCHY(
            HttpStatus.BAD_REQUEST, "Parent hierarchy must be exactly one level above the child issue"),
    STORY_POINT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Story points are not supported for this hierarchy"),
    PARENT_REQUIRED(HttpStatus.BAD_REQUEST, "Issues of this hierarchy require a parent and cannot stand alone"),
    PARENT_PROJECT_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "Cross project parent-child relations are only allowed when the parent is EPIC hierarchy"),
    ISSUE_SELF_REFERENCE(HttpStatus.BAD_REQUEST, "An issue cannot reference itself"),
    TRANSITION_SOURCE_STATE_NOT_MATCH(
            HttpStatus.BAD_REQUEST, "Issue's current state does not match the required source state for transition"),
    ISSUE_IN_PROGRESS_DELETION_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST, "Cannot delete issue that is not in the initial state"),
    CANNOT_DELETE_ISSUE_WITH_CHILDREN(HttpStatus.BAD_REQUEST, "Cannot delete issue that has child issues"),
    DUE_DATE_MUST_BE_FUTURE(HttpStatus.BAD_REQUEST, "Due date must be in the future"),
    INVALID_PERCENTAGE_EXCEPTION(HttpStatus.BAD_REQUEST, "Percentage must be a value of 0 ~ 100"),
    REVIEWER_NOT_FOUND(HttpStatus.NOT_FOUND, "Reviewer not found in issue participants"),
    RELATION_CIRCULAR_DEPENDENCY(HttpStatus.BAD_REQUEST, "Circular dependency detected in the issue relation graph"),
    RELATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "A relation already exists between these two issues"),
    RELATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Relation not found between these two issues"),
    CUSTOM_FIELD_REQUIRED(HttpStatus.BAD_REQUEST, "Required custom field is missing or empty"),
    CUSTOM_FIELD_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "Invalid value format for the custom field"),
    UNKNOWN_CUSTOM_FIELD_ID(HttpStatus.BAD_REQUEST, "The provided custom field ID is unknown"),
    UNKNOWN_ENUM_OPTION(HttpStatus.BAD_REQUEST, "Unknown enum option for field"),
    DECIMAL_FRACTION_PART_TOO_LONG(HttpStatus.BAD_REQUEST, "Field value exceeds maximum allowed fraction digits"),
    DECIMAL_INTEGER_PART_TOO_LONG(HttpStatus.BAD_REQUEST, "Field value exceeds maximum allowed integer digits"),
    MAX_REVIEWERS_EXCEEDED(HttpStatus.CONFLICT, "Maximum number of reviewers reached"),
    ASSIGNEE_CANNOT_BE_REVIEWER(HttpStatus.BAD_REQUEST, "The assignee cannot also be a reviewer of the same issue"),
    ISSUE_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "The issue is already assigned to another member"),
    ISSUE_DELETE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "Insufficient permission to delete this issue"),

    // Search
    UNSUPPORTED_SORT_PROPERTY(HttpStatus.BAD_REQUEST, "Sort property is not allowed"),
    INVALID_CURSOR_TOKEN(HttpStatus.BAD_REQUEST, "Cursor token is malformed"),

    // Attachment
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Attachment not found"),
    ATTACHMENT_DELETE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "Must be the uploader or admin to delete the attachment"),
    ATTACHMENT_FILE_EMPTY(HttpStatus.BAD_REQUEST, "Uploaded file is empty"),
    ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Content type is not allowed"),
    ATTACHMENT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "Maximum number of attachments per issue exceeded"),
    ATTACHMENT_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the attachment file");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
