package com.tissue.position.domain.exception;

import com.tissue.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PositionErrorCode implements ErrorCode {
    POSITION_NOT_FOUND("Position not found"),
    DUPLICATE_POSITION_NAME("Position name must be unique for workspace"),
    POSITION_IN_USE("Postion is in use by a workspace member");

    private final String defaultMessage;
}
