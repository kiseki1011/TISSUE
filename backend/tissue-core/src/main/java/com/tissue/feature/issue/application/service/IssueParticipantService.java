package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.IssueIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueParticipantService implements IssueParticipantUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final IssuePolicy issuePolicy;
    private final IssueAuthorizationService issueAuthService;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void assign(IssueIdentifier issueIdentifier, Long targetMemberId, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        ProjectMember assignee = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.assignTo(assignee);

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishAssigned(issue, assignee, actor);
    }

    @Override
    public void unassign(IssueIdentifier issueIdentifier, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        ProjectMember assignee = issue.getParticipants().getAssignee();
        if (assignee == null) {
            return;
        }

        issue.unassign();

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishUnassigned(issue, assignee, actor);
    }

    @Override
    public void subscribe(IssueIdentifier issueIdentifier, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        ProjectMember subscriber = projectMemberFinder.getBy(issue.getProject(), memberId);
        issue.addSubscriber(subscriber);
    }

    @Override
    public void unsubscribe(IssueIdentifier issueIdentifier, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        ProjectMember subscriber = projectMemberFinder.getBy(issue.getProject(), memberId);
        issue.removeSubscriber(subscriber);
    }

    @Override
    public void addReviewer(IssueIdentifier issueIdentifier, Long targetMemberId, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        issuePolicy.ensureCanAddReviewer(issue);

        ProjectMember reviewer = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.addReviewer(reviewer);

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishReviewerAdded(issue, reviewer, actor);
    }

    @Override
    public void removeReviewer(IssueIdentifier issueIdentifier, Long targetMemberId, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        ProjectMember reviewer = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.removeReviewer(reviewer);

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishReviewerRemoved(issue, reviewer, actor);
    }
}
