package com.tissue.common.exception.domain;

import com.tissue.common.exception.base.BadRequestException;

public class SelfOperationNotAllowedException extends BadRequestException {

	public SelfOperationNotAllowedException(String message) {
		super(message);
	}
}
