package com.tissue.feature.issue.domain.exception;

import com.tissue.shared.exception.base.InternalServerException;

/**
 * Thrown when a required Spring Converter is not registered for a specific custom field type.
 */
public class IssueFieldConverterNotFoundException extends InternalServerException {
    public IssueFieldConverterNotFoundException(String debugMessage) {
        super(IssueErrorCode.ISSUE_FIELD_CONVERTER_NOT_FOUND, debugMessage);
    }
}
