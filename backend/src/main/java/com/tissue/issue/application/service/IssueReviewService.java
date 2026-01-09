package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.SubmitReviewCommand;
import com.tissue.issue.application.port.in.IssueReviewUseCase;
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueReviewer;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueReviewService implements IssueReviewUseCase {

    private final IssueFinder issueFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final CurrentMemberProvider currentMemberProvider;
    private final IssueAuthorizationService issueAuthService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void submitReview(SubmitReviewCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);
        ProjectMember actor = projectMemberFinder.getBy(issue.getProject(), cmd.actorMemberId());

        // TODO: since im finding whether the actor is a reviewer i dont need to add the method
        //  in the IssueAuthorizationService?
        //  Or should i separate this logic to a method like IssueAuthorizationService.requireReviewSubmitPermission
        IssueReviewer reviewer = findReviewerEntry(issue, actor);

        if (cmd.approved()) {
            reviewer.approve();
        } else {
            reviewer.reject();
        }

        eventPublisher.publishEvent(IssueReviewSubmittedEvent.create(issue, reviewer.getStatus(), actor));
    }

    private IssueReviewer findReviewerEntry(Issue issue, ProjectMember actor) {
        return issue.getParticipants().getReviewers().stream()
                .filter(r -> r.getReviewer().equals(actor))
                .findFirst()
                .orElseThrow(() -> IssueExceptions.reviewerNotFound(actor.getMemberId()));
    }
}
