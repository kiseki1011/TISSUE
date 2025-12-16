package com.tissue.common.dto;

public record FieldChange(
	Object from,
	Object to
) {
	public static FieldChange of(Object from, Object to) {
		return new FieldChange(from, to);
	}
}
