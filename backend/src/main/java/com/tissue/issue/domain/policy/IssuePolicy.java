package com.tissue.issue.domain.policy;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueExceptions;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IssuePolicy {

    private final int maxReviewers;

    // TODO: 아래와 같은 방식 말고 currentCount를 파라미터로 받는 방식으로 변경할까?
    public void ensureCanAddReviewer(Issue issue) {
        if (issue.getParticipants().getReviewers().size() >= maxReviewers) {
            throw IssueExceptions.maxReviewersExceeded(maxReviewers);
        }
    }
}
