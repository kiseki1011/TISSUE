package com.tissue.feature.tag.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TagErrorCode implements ErrorCode {
    TAG_NOT_FOUND("Tag not found"),
    DUPLICATE_TAG_NAME("Tag name already exists in project"),
    TAG_IN_USE("Tag is currently in use by issues");

    private final String defaultMessage;
}
