package com.tissue.api.common.exception.domain;

import com.tissue.api.common.exception.base.BadRequestException;

public class SelfOperationNotAllowedException extends BadRequestException {

	public SelfOperationNotAllowedException(String message) {
		super(message);
	}
}
