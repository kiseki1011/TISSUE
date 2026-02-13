package com.tissue.feature.issue.domain;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.INVALID_PERCENTAGE_EXCEPTION;

import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
@Embeddable
public class IssueProgress {

    private static final int MIN_PERCENTAGE = 0;
    private static final int MAX_PERCENTAGE = 100;

    @Nullable
    @Column(name = "count_based_progress")
    private Integer countBasedProgress;

    @Nullable
    @Column(name = "point_based_progress")
    private Integer pointBasedProgress;

    @SuppressWarnings("NullAway.Init")
    protected IssueProgress() {}

    static IssueProgress init() {
        return new IssueProgress();
    }

    void update(@Nullable Integer countBased, @Nullable Integer pointBased) {
        this.countBasedProgress = ensureValidPercentageRange(countBased);
        this.pointBasedProgress = ensureValidPercentageRange(pointBased);
    }

    private @Nullable Integer ensureValidPercentageRange(@Nullable Integer value) {
        if (value == null) {
            return null;
        }
        if (value < MIN_PERCENTAGE || value > MAX_PERCENTAGE) {
            throw new BadRequestException(INVALID_PERCENTAGE_EXCEPTION);
        }

        return value;
    }
}
