package com.tissue.issue.application.service;

import com.tissue.issue.application.port.in.IssueReviewUseCase;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueReviewer;
import com.tissue.issue.domain.exception.ReviewerNotFoundException;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.ProjectMember;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueReviewService implements IssueReviewUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void submitReview(String issueKey, boolean approved, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);
        ProjectMember actor = projectMemberFinder.getBy(issue.getProject(), actorContext.memberId());
        IssueReviewer reviewer = findReviewerEntry(issue, actor);

        if (approved) {
            reviewer.approve();
        } else {
            reviewer.reject();
        }

        eventPublisher.publishReviewSubmitted(issue, reviewer.getStatus(), actorContext);
    }

    @Override
    public void requestReview(String issueKey, Set<Long> reviewerMemberIds, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        int count = issue.resetReviews(reviewerMemberIds);

        eventPublisher.publishReviewRequested(issue, actorContext, reviewerMemberIds, count);
    }

    private IssueReviewer findReviewerEntry(Issue issue, ProjectMember actor) {
        return issue.getParticipants().getReviewers().stream()
                .filter(r -> r.getReviewer().equals(actor))
                .findFirst()
                .orElseThrow(() -> new ReviewerNotFoundException(actor.getMemberId()));
    }
}
