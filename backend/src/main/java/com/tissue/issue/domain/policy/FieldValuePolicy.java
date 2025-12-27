package com.tissue.issue.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.tissue.issue.domain.exception.IssueExceptions;

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
			throw IssueExceptions.decimalScaleExceeded(fieldId, maxFractionDigits);
		}
		int precision = abs.precision();
		int integerDigits = Math.max(0, precision - scale);
		if (integerDigits > maxIntegerDigits) {
			throw IssueExceptions.integerDigitsExceeded(fieldId, maxIntegerDigits);
		}
	}

	public BigDecimal normalizeDecimal(BigDecimal input) {
		if (input == null) {
			return null;
		}
		return input.setScale(decimalScale, roundingMode);
	}
}
