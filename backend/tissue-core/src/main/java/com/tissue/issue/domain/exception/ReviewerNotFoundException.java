package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.exception.base.ResourceNotFoundException;

public class ReviewerNotFoundException extends ResourceNotFoundException {

    public ReviewerNotFoundException(Long memberId) {
        super(IssueErrorCode.REVIEWER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
