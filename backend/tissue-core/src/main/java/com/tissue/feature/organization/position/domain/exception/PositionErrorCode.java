package com.tissue.feature.organization.position.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PositionErrorCode implements ErrorCode {
    POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "Position not found"),
    DUPLICATE_POSITION_NAME(HttpStatus.CONFLICT, "Position name already exists");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
