package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.usecase.IssueReviewUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.exception.ReviewerNotFoundException;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueReviewService implements IssueReviewUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void submitReview(IssueIdentifier issueIdentifier, boolean approved, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());
        ProjectMember actor = projectMemberFinder.getBy(issue.getProject(), memberId);
        IssueReviewer reviewer = findReviewerEntry(issue, actor);

        if (approved) {
            reviewer.approve();
        } else {
            reviewer.reject();
        }

        WorkspaceMember workspaceActor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishReviewSubmitted(issue, reviewer.getStatus(), workspaceActor);
    }

    @Override
    public void requestReview(IssueIdentifier issueIdentifier, Set<Long> reviewerMemberIds, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        int count = issue.resetReviews(reviewerMemberIds);

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishReviewRequested(issue, actor, reviewerMemberIds, count);
    }

    private IssueReviewer findReviewerEntry(Issue issue, ProjectMember actor) {
        return issue.getParticipants().getReviewers().stream()
                .filter(r -> r.getReviewer().equals(actor))
                .findFirst()
                .orElseThrow(() -> new ReviewerNotFoundException(actor.getMemberId()));
    }
}
