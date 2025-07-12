package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class InvalidConfigurationException extends TissueException {

	public InvalidConfigurationException(String message) {
		super(message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public InvalidConfigurationException(String message, Throwable cause) {
		super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
	}
}
