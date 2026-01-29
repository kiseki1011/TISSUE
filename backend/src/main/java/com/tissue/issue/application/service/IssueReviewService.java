package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.RequestReviewCommand;
import com.tissue.issue.application.dto.request.SubmitReviewCommand;
import com.tissue.issue.application.port.in.IssueReviewUseCase;
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueReviewer;
import com.tissue.issue.domain.exception.ReviewerNotFoundException;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueReviewService implements IssueReviewUseCase {

    private final IssueFinder issueFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueAuthorizationService issueAuthService;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void submitReview(SubmitReviewCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);
        ProjectMember actor = projectMemberFinder.getIncludingSoftDeleted(issue.getProject(), actorContext.memberId());

        IssueReviewer reviewer = findReviewerEntry(issue, actor);

        if (cmd.approved()) {
            reviewer.approve();
        } else {
            reviewer.reject();
        }

        eventPublisher.publishReviewSubmitted(issue, reviewer.getStatus(), actorContext);
    }

    @Override
    public void requestReview(RequestReviewCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireIssueEditPermission(issue, actorContext);

        int count = issue.resetReviews(cmd.reviewerMemberIds());

        eventPublisher.publishReviewRequested(issue, actorContext, cmd.reviewerMemberIds(), count);
    }

    private IssueReviewer findReviewerEntry(Issue issue, ProjectMember actor) {
        return issue.getParticipants().getReviewers().stream()
                .filter(r -> r.getReviewer().equals(actor))
                .findFirst()
                .orElseThrow(() -> new ReviewerNotFoundException(actor.getMemberId()));
    }
}
