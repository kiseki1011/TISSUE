package com.tissue.feature.issuetype.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IssueTypeErrorCode implements ErrorCode {
    ISSUE_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "Issue type not found"),
    ISSUE_FIELD_NOT_FOUND(HttpStatus.NOT_FOUND, "Issue field not found"),
    FIELD_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Field option not found"),

    DUPLICATE_ISSUE_TYPE_NAME(HttpStatus.CONFLICT, "Issue type name already exists"),
    DUPLICATE_ISSUE_FIELD_NAME(HttpStatus.CONFLICT, "Issue field name already exists in issue type"),
    DUPLICATE_FIELD_OPTION_NAME(HttpStatus.CONFLICT, "Option label already exists in field"),

    ISSUE_TYPE_IN_USE(HttpStatus.CONFLICT, "Issue type is currently in use"),
    ISSUE_FIELD_IN_USE(HttpStatus.CONFLICT, "Issue field is currently in use"),
    ISSUE_FIELD_OPTION_IN_USE(HttpStatus.CONFLICT, "Field option is currently in use"),

    FIELD_TYPE_CANNOT_HAVE_OPTION(HttpStatus.BAD_REQUEST, "This field type cannot add options"),
    OPTION_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "Maximum number of options exceeded"),
    OPTION_REORDER_UNKNOWN_ID(HttpStatus.BAD_REQUEST, "Provided option ID does not exist in this field");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
