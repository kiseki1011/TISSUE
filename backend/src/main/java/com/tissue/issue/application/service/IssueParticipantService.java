package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.AddReviewerCommand;
import com.tissue.issue.application.dto.request.AssignIssueCommand;
import com.tissue.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.issue.application.dto.request.UnsubscribeIssueCommand;
import com.tissue.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.policy.IssuePolicy;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.ProjectMember;
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
    public void assign(AssignIssueCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), cmd.issueKey());

        issueAuthService.requireParticipantManagePermission(issue, actorContext);

        ProjectMember assignee = projectMemberFinder.getBy(issue.getProject(), cmd.targetMemberId());
        issue.assignTo(assignee);

        eventPublisher.publishAssigned(issue, assignee, actorContext);
    }

    @Override
    public void unassign(RemoveAssigneeCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), cmd.issueKey());

        issueAuthService.requireParticipantManagePermission(issue, actorContext);

        ProjectMember assignee = issue.getParticipants().getAssignee();
        if (assignee == null) {
            return;
        }

        issue.unassign();

        eventPublisher.publishUnassigned(issue, assignee, actorContext);
    }

    @Override
    public void subscribe(SubscribeIssueCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), cmd.issueKey());

        ProjectMember subscriber = projectMemberFinder.getBy(issue.getProject(), actorContext.memberId());
        issue.addSubscriber(subscriber);
    }

    @Override
    public void unsubscribe(UnsubscribeIssueCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), cmd.issueKey());

        ProjectMember subscriber = projectMemberFinder.getBy(issue.getProject(), actorContext.memberId());
        issue.removeSubscriber(subscriber);
    }

    @Override
    public void addReviewer(AddReviewerCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), cmd.issueKey());

        issueAuthService.requireReviewerManagePermission(issue, actorContext);
        issuePolicy.ensureCanAddReviewer(issue);

        ProjectMember reviewer = projectMemberFinder.getBy(issue.getProject(), cmd.targetMemberId());
        issue.addReviewer(reviewer);

        eventPublisher.publishReviewerAdded(issue, reviewer, actorContext);
    }

    @Override
    public void removeReviewer(RemoveReviewerCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), cmd.issueKey());

        issueAuthService.requireReviewerManagePermission(issue, actorContext);

        ProjectMember reviewer = projectMemberFinder.getBy(issue.getProject(), cmd.targetMemberId());
        issue.removeReviewer(reviewer);

        eventPublisher.publishReviewerRemoved(issue, reviewer, actorContext);
    }
}
