package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceExcpetion extends TissueException {
	public DuplicateResourceExcpetion(String message) {
		super(message, HttpStatus.CONFLICT);
	}
}
