package com.tissue.api.issue.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record FieldValuePolicy(
	int decimalScale,
	RoundingMode roundingMode,
	int maxIntegerDigits,
	int maxFractionDigits
) {
	public void ensureDigits(BigDecimal value, Long fieldId) {
		if (value == null) {
			return;
		}
		BigDecimal abs = value.abs();
		int scale = abs.scale();
		if (scale > maxFractionDigits) {
			throw new RuntimeException(
				"Field(id: '%d') allows up to %d fraction digits."
					.formatted(fieldId, maxFractionDigits));
		}
		int precision = abs.precision();
		int integerDigits = Math.max(0, precision - scale);
		if (integerDigits > maxIntegerDigits) {
			throw new RuntimeException(
				"Field(id: '%d') allows up to %d integer digits."
					.formatted(fieldId, maxIntegerDigits));
		}
	}

	public BigDecimal normalizeDecimal(BigDecimal input) {
		if (input == null) {
			return null;
		}
		return input.setScale(decimalScale, roundingMode);
	}
}
