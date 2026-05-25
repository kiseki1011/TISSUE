package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ALLOWED_SORT_PROPERTIES;
import static com.tissue.shared.exception.ErrorContextKeys.SORT_PROPERTY;

import com.tissue.shared.exception.base.BadRequestException;
import java.util.Set;

public class InvalidSortPropertyException extends BadRequestException {

    public InvalidSortPropertyException(String sortProperty, Set<String> allowedSortProperties) {
        super(IssueErrorCode.UNSUPPORTED_SORT_PROPERTY);
        addContext(SORT_PROPERTY, sortProperty);
        addContext(ALLOWED_SORT_PROPERTIES, allowedSortProperties);
    }
}
