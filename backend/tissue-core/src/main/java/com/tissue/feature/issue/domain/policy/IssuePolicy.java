package com.tissue.feature.issue.domain.policy;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.MAX_REVIEWERS_EXCEEDED;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.shared.exception.base.BadRequestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IssuePolicy {

    private final int maxReviewers;

    public void ensureCanAddReviewer(Issue issue) {
        if (issue.getParticipants().getReviewers().size() >= maxReviewers) {
            throw new BadRequestException(MAX_REVIEWERS_EXCEEDED).addContext("maxReviewers", maxReviewers);
        }
    }
}
