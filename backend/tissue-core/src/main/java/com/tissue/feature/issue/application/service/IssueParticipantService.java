package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.feature.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueParticipantService implements IssueParticipantUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssuePolicy issuePolicy;
    private final IssueAuthorizationService issueAuthService;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void assign(String issueKey, Long targetMemberId, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        ProjectMember assignee = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.assignTo(assignee);

        eventPublisher.publishAssigned(issue, assignee, actorContext);
    }

    @Override
    public void unassign(String issueKey, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        ProjectMember assignee = issue.getParticipants().getAssignee();
        if (assignee == null) {
            return;
        }

        issue.unassign();

        eventPublisher.publishUnassigned(issue, assignee, actorContext);
    }

    @Override
    public void subscribe(String issueKey, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        ProjectMember subscriber = projectMemberFinder.getBy(issue.getProject(), actorContext.memberId());
        issue.addSubscriber(subscriber);
    }

    @Override
    public void unsubscribe(String issueKey, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        ProjectMember subscriber = projectMemberFinder.getBy(issue.getProject(), actorContext.memberId());
        issue.removeSubscriber(subscriber);
    }

    @Override
    public void addReviewer(String issueKey, Long targetMemberId, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        issuePolicy.ensureCanAddReviewer(issue);

        ProjectMember reviewer = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.addReviewer(reviewer);

        eventPublisher.publishReviewerAdded(issue, reviewer, actorContext);
    }

    @Override
    public void removeReviewer(String issueKey, Long targetMemberId, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        ProjectMember reviewer = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.removeReviewer(reviewer);

        eventPublisher.publishReviewerRemoved(issue, reviewer, actorContext);
    }
}
