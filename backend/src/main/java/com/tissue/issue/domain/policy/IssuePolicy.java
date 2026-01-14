package com.tissue.issue.domain.policy;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.MaxReviewersExceededException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IssuePolicy {

    private final int maxReviewers;

    public void ensureCanAddReviewer(Issue issue) {
        if (issue.getParticipants().getReviewers().size() >= maxReviewers) {
            throw new MaxReviewersExceededException(maxReviewers);
        }
    }
}
