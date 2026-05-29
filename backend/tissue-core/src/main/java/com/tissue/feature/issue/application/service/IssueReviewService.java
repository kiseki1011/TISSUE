package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.usecase.IssueReviewUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.exception.ReviewerNotFoundException;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueReviewService implements IssueReviewUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void submitReview(IssueIdentifier iid, boolean approved, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());
        IssueReviewer reviewer = findReviewerEntry(issue, actor);

        if (approved) {
            reviewer.approve();
        } else {
            reviewer.reject();
        }

        eventPublisher.publishReviewSubmitted(issue, reviewer.getStatus(), actor);
    }

    @Override
    public void requestReview(IssueIdentifier iid, Set<Long> reviewerMemberIds, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        int count = issue.resetReviews(reviewerMemberIds);

        eventPublisher.publishReviewRequested(issue, actor, reviewerMemberIds, count);
    }

    private IssueReviewer findReviewerEntry(Issue issue, ProjectMember actor) {
        return issue.getParticipants().getReviewers().stream()
                .filter(r -> r.getReviewer().equals(actor))
                .findFirst()
                .orElseThrow(() -> new ReviewerNotFoundException(actor.getMemberId()));
    }
}
