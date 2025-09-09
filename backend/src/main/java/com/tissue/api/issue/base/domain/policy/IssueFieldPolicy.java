package com.tissue.api.issue.base.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.common.exception.type.InvalidOperationException;

public record IssueFieldPolicy(
	int maxEnumOptions,
	int decimalScale,
	RoundingMode roundingMode,
	int maxIntegerDigits,
	int maxFractionDigits
) {
	public void ensureOptionsWithinLimit(List<String> options) {
		if (options.size() > maxEnumOptions) {
			throw new InvalidOperationException("Too many options. max=" + maxEnumOptions);
		}
	}

	public void ensureCanAddOption(int activeCount) {
		if (activeCount >= maxEnumOptions) {
			throw new InvalidOperationException("Too many options. max=" + maxEnumOptions);
		}
	}

	public void ensureDigits(BigDecimal value, String fieldKey) {
		if (value == null) {
			return;
		}
		BigDecimal abs = value.abs();
		int scale = abs.scale();
		if (scale > maxFractionDigits) {
			throw new InvalidCustomFieldException(
				"Field '%s' allows up to %d fraction digits."
					.formatted(fieldKey, maxFractionDigits));
		}
		int precision = abs.precision();
		int integerDigits = Math.max(0, precision - scale);
		if (integerDigits > maxIntegerDigits) {
			throw new InvalidCustomFieldException(
				"Field '%s' allows up to %d integer digits."
					.formatted(fieldKey, maxIntegerDigits));
		}
	}

	public BigDecimal normalizeDecimal(BigDecimal input) {
		if (input == null) {
			return null;
		}
		return input.setScale(decimalScale, roundingMode);
	}
}
