package com.tissue.common.exception.domain;

import com.tissue.common.exception.base.BadRequestException;

public class ValueOutOfRangeException extends BadRequestException {

	public ValueOutOfRangeException(int value, int min, int max) {
		super("Value must be between %d and %d.".formatted(min, max));
		addContext("value", value);
		addContext("min", min);
		addContext("max", max);
	}
}
