package com.tissue.api.common.exception.domain;

import com.tissue.api.common.exception.base.BadRequestException;

public class SizeLimitExceededException extends BadRequestException {

	public SizeLimitExceededException(String collection, int current, int max) {
		super("Maximum %s limit reached".formatted(collection));
		addContext("collectionName", collection);
		addContext("currentSize", current);
		addContext("maxSize", max);
	}
}
