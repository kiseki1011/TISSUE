package com.tissue.feature.tag.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TagErrorCode implements ErrorCode {
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "Tag not found"),
    DUPLICATE_TAG_NAME(HttpStatus.CONFLICT, "Tag name already exists in project");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
