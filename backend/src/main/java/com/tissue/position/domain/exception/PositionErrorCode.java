package com.tissue.position.domain.exception;

import com.tissue.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PositionErrorCode implements ErrorCode {

	POSITION_NOT_FOUND("Position not found");

	private final String defaultMessage;
}
