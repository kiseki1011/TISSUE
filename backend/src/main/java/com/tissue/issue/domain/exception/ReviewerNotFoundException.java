package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.MEMBER_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class ReviewerNotFoundException extends ResourceNotFoundException {

    public ReviewerNotFoundException(Long memberId) {
        super(IssueErrorCode.REVIEWER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
