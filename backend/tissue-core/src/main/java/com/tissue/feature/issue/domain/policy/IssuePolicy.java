package com.tissue.feature.issue.domain.policy;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.DECIMAL_FRACTION_PART_TOO_LONG;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.DECIMAL_INTEGER_PART_TOO_LONG;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.MAX_REVIEWERS_EXCEEDED;
import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.OPTION_LIMIT_EXCEEDED;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record IssuePolicy(
        int maxReviewers,
        int decimalScale,
        RoundingMode decimalRounding,
        int decimalMaxIntegerDigits,
        int decimalMaxFractionDigits,
        int maxSelectOptions) {

    public void ensureCanAddReviewer(Issue issue) {
        if (issue.getParticipants().getReviewers().size() >= maxReviewers) {
            throw new ResourceConflictException(MAX_REVIEWERS_EXCEEDED).addContext("maxReviewers", maxReviewers);
        }
    }

    public void ensureCanAddOption(int currentCount) {
        if (currentCount >= maxSelectOptions) {
            throw new ResourceConflictException(OPTION_LIMIT_EXCEEDED).addContext("maxOptions", maxSelectOptions);
        }
    }

    public void ensureDigits(BigDecimal bd) {
        int integerPartLength = bd.precision() - bd.scale();
        if (integerPartLength > decimalMaxIntegerDigits) {
            throw new BadRequestException(DECIMAL_INTEGER_PART_TOO_LONG).addContext("max", decimalMaxIntegerDigits);
        }
        if (bd.scale() > decimalMaxFractionDigits) {
            throw new BadRequestException(DECIMAL_FRACTION_PART_TOO_LONG).addContext("max", decimalMaxFractionDigits);
        }
    }

    public BigDecimal normalizeDecimal(BigDecimal bd) {
        return bd.setScale(decimalScale, decimalRounding);
    }
}
