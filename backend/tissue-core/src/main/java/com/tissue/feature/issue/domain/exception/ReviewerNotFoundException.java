package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class ReviewerNotFoundException extends ResourceNotFoundException {

    public ReviewerNotFoundException(Long memberId) {
        super(IssueErrorCode.REVIEWER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
