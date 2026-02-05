package com.tissue.issue.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.Set;

public interface IssueReviewUseCase {

    void submitReview(String issueKey, boolean approved, ProjectMemberContext actorContext);

    void requestReview(String issueKey, Set<Long> reviewerMemberIds, ProjectMemberContext actorContext);
}
