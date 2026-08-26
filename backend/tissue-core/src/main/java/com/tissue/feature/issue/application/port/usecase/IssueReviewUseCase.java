package com.tissue.feature.issue.application.port.usecase;

import com.tissue.shared.dto.IssueIdentifier;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public interface IssueReviewUseCase {

    /**
     * Records the reviewer's verdict. A non-blank {@code comment} is also stored as the review's feedback
     * body, visible in the issue's comment thread; a blank or null one leaves only the status change.
     */
    void submitReview(IssueIdentifier iid, boolean approved, @Nullable String comment, Long actorMemberId);

    void requestReview(IssueIdentifier iid, Set<Long> reviewerMemberIds, Long actorMemberId);
}
