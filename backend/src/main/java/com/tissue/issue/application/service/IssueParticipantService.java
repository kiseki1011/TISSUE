package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.AddReviewerCommand;
import com.tissue.issue.application.dto.request.AssignIssueCommand;
import com.tissue.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.issue.application.dto.request.UnsubscribeIssueCommand;
import com.tissue.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueReporterChangedEvent;
import com.tissue.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.issue.domain.event.IssueUnassignedEvent;
import com.tissue.issue.domain.policy.IssuePolicy;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueParticipantService implements IssueParticipantUseCase {

    private final IssueFinder issueFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssuePolicy issuePolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void changeReporter(ChangeReporterCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);

        ProjectMember oldReporter = issue.getParticipants().getReporter();
        ProjectMember newReporter = projectMemberFinder.findBy(project, cmd.targetMemberId());
        ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

        issue.changeReporter(newReporter);

        eventPublisher.publishEvent(
                IssueReporterChangedEvent.create(issue, oldReporter, newReporter, actor));
    }

    @Override
    @Transactional
    public void assign(AssignIssueCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);

        ProjectMember assignee = projectMemberFinder.findBy(project, cmd.targetMemberId());
        ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

        issue.assignTo(assignee);

        eventPublisher.publishEvent(IssueAssignedEvent.create(issue, assignee, actor));
    }

    @Override
    @Transactional
    public void unassign(RemoveAssigneeCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);

        ProjectMember assignee = issue.getParticipants().getAssignee();
        ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

        issue.unassign();

        eventPublisher.publishEvent(IssueUnassignedEvent.create(issue, assignee, actor));
    }

    @Override
    @Transactional
    public void subscribe(SubscribeIssueCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);

        ProjectMember subscriber = projectMemberFinder.findBy(project, cmd.actorMemberId());

        issue.addSubscriber(subscriber);
    }

    @Override
    @Transactional
    public void unsubscribe(UnsubscribeIssueCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);

        ProjectMember subscriber = projectMemberFinder.findBy(project, cmd.actorMemberId());

        issue.removeSubscriber(subscriber);
    }

    @Override
    @Transactional
    public void addReviewer(AddReviewerCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);

        ProjectMember reviewer = projectMemberFinder.findBy(project, cmd.targetMemberId());
        ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

        issuePolicy.ensureCanAddReviewer(issue);
        issue.addReviewer(reviewer);

        eventPublisher.publishEvent(IssueReviewerAddedEvent.create(issue, reviewer, actor));
    }

    @Override
    @Transactional
    public void removeReviewer(RemoveReviewerCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);

        ProjectMember reviewer = projectMemberFinder.findBy(project, cmd.targetMemberId());
        ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

        issue.removeReviewer(reviewer);

        eventPublisher.publishEvent(IssueReviewerRemovedEvent.create(issue, reviewer, actor));
    }
}
