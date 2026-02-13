package com.tissue.feature.issue.domain.policy;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.DECIMAL_SCALE_EXCEEDED;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.INTEGER_DIGITS_EXCEEDED;

import com.tissue.shared.exception.base.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jspecify.annotations.Nullable;

public record FieldValuePolicy(
        int decimalScale, RoundingMode roundingMode, int maxIntegerDigits, int maxFractionDigits) {

    public void ensureDigits(BigDecimal value) {
        if (value == null) {
            return;
        }

        BigDecimal abs = value.abs();
        int scale = abs.scale();
        if (scale > maxFractionDigits) {
            throw new BadRequestException(DECIMAL_SCALE_EXCEEDED).addContext("maxFractionDigits", maxFractionDigits);
        }

        int precision = abs.precision();
        int integerDigits = Math.max(0, precision - scale);
        if (integerDigits > maxIntegerDigits) {
            throw new BadRequestException(INTEGER_DIGITS_EXCEEDED).addContext("maxIntegerDigits", maxIntegerDigits);
        }
    }

    public @Nullable BigDecimal normalizeDecimal(BigDecimal input) {
        if (input == null) {
            return null;
        }
        return input.setScale(decimalScale, roundingMode);
    }
}
