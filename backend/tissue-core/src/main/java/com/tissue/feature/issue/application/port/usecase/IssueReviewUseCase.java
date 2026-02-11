package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.project.application.dto.ProjectMemberContext;
import java.util.Set;

public interface IssueReviewUseCase {

    void submitReview(String issueKey, boolean approved, ProjectMemberContext actorContext);

    void requestReview(String issueKey, Set<Long> reviewerMemberIds, ProjectMemberContext actorContext);
}
