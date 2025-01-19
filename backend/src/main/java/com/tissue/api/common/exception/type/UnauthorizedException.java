package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class UnauthorizedException extends TissueException {
	public UnauthorizedException(String message) {
		super(message, HttpStatus.UNAUTHORIZED);
	}
}
