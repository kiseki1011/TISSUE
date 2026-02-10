package com.tissue.feature.issue.domain.policy;

import com.tissue.feature.issue.domain.exception.DecimalScaleExceededException;
import com.tissue.feature.issue.domain.exception.IntegerDigitsExceededException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jspecify.annotations.Nullable;

public record FieldValuePolicy(
        int decimalScale, RoundingMode roundingMode, int maxIntegerDigits, int maxFractionDigits) {

    public void ensureDigits(BigDecimal value, Long fieldId) {
        if (value == null) {
            return;
        }
        BigDecimal abs = value.abs();
        int scale = abs.scale();
        if (scale > maxFractionDigits) {
            throw new DecimalScaleExceededException(fieldId, maxFractionDigits);
        }
        int precision = abs.precision();
        int integerDigits = Math.max(0, precision - scale);
        if (integerDigits > maxIntegerDigits) {
            throw new IntegerDigitsExceededException(fieldId, maxIntegerDigits);
        }
    }

    public @Nullable BigDecimal normalizeDecimal(BigDecimal input) {
        if (input == null) {
            return null;
        }
        return input.setScale(decimalScale, roundingMode);
    }
}
