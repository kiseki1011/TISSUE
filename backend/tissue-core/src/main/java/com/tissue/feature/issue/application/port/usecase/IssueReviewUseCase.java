package com.tissue.feature.issue.application.port.usecase;

import com.tissue.shared.dto.IssueIdentifier;
import java.util.Set;

public interface IssueReviewUseCase {

    void submitReview(IssueIdentifier issueIdentifier, boolean approved, Long memberId);

    void requestReview(IssueIdentifier issueIdentifier, Set<Long> reviewerMemberIds, Long memberId);
}
