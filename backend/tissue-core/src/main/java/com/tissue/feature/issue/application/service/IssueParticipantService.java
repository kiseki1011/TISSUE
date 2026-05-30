package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
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
    private final IssuePolicy issuePolicy;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void assign(IssueIdentifier iid, Long targetMemberId, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        ProjectMember assignee = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.assignTo(assignee);

        eventPublisher.publishAssigned(issue, assignee, actor);
    }

    @Override
    public void unassign(IssueIdentifier iid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        ProjectMember assignee = issue.getParticipants().getAssignee();
        if (assignee == null) {
            return;
        }

        issue.unassign();

        eventPublisher.publishUnassigned(issue, assignee, actor);
    }

    @Override
    public void subscribe(IssueIdentifier iid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        issue.addSubscriber(actor);
    }

    @Override
    public void unsubscribe(IssueIdentifier iid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        issue.removeSubscriber(actor);
    }

    @Override
    public void addReviewer(IssueIdentifier iid, Long targetMemberId, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        issuePolicy.ensureCanAddReviewer(issue);

        ProjectMember reviewer = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.addReviewer(reviewer);

        eventPublisher.publishReviewerAdded(issue, reviewer, actor);
    }

    @Override
    public void removeReviewer(IssueIdentifier iid, Long targetMemberId, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        ProjectMember reviewer = projectMemberFinder.getBy(issue.getProject(), targetMemberId);
        issue.removeReviewer(reviewer);

        eventPublisher.publishReviewerRemoved(issue, reviewer, actor);
    }
}
