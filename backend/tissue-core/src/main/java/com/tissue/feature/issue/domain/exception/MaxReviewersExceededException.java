package com.tissue.feature.issue.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;

public class MaxReviewersExceededException extends BadRequestException {

    public MaxReviewersExceededException(int maxReviewers) {
        super(IssueErrorCode.MAX_REVIEWERS_EXCEEDED);
        addContext("maxReviewers", maxReviewers);
    }
}
