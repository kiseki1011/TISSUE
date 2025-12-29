package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.SubmitReviewCommand;
import com.tissue.issue.application.port.in.IssueReviewUseCase;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueReviewer;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueReviewService implements IssueReviewUseCase {

    private final IssueFinder issueFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void submitReview(SubmitReviewCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);
        ProjectMember actor = projectMemberFinder.findBy(issue.getProject(), cmd.actorMemberId());

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
