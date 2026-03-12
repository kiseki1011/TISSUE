package com.tissue.feature.issue.application.port.usecase;

import com.tissue.shared.dto.IssueIdentifier;
import java.util.Set;

public interface IssueReviewUseCase {

    void submitReview(IssueIdentifier iid, boolean approved, Long actorMemberId);

    void requestReview(IssueIdentifier iid, Set<Long> reviewerMemberIds, Long actorMemberId);
}
