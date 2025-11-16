package com.tissue.api.common.exception.domain;

import com.tissue.api.common.exception.base.BadRequestException;

public class InvalidPercentageException extends BadRequestException {

	public InvalidPercentageException(int value, int min, int max) {
		super("Percentage must be between %d and %d.".formatted(min, max));
		addContext("value", value);
		addContext("min", min);
		addContext("max", max);
	}
}
