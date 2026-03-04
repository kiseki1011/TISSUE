package com.tissue.feature.issue.application.service;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.REVIEWER_NOT_FOUND;

import com.tissue.feature.issue.application.port.usecase.IssueReviewUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.exception.base.ResourceNotFoundException;
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
    public void submitReview(IssueIdentifier issueIdentifier, boolean approved, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());
        IssueReviewer reviewer = findReviewerEntry(issue, actor);

        if (approved) {
            reviewer.approve();
        } else {
            reviewer.reject();
        }

        eventPublisher.publishReviewSubmitted(issue, reviewer.getStatus(), actor);
    }

    @Override
    public void requestReview(IssueIdentifier issueIdentifier, Set<Long> reviewerMemberIds, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        int count = issue.resetReviews(reviewerMemberIds);

        eventPublisher.publishReviewRequested(issue, actor, reviewerMemberIds, count);
    }

    private IssueReviewer findReviewerEntry(Issue issue, ProjectMember actor) {
        return issue.getParticipants().getReviewers().stream()
                .filter(r -> r.getReviewer().equals(actor))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(REVIEWER_NOT_FOUND).addContext("memberId", actor.getMemberId()));
    }
}
