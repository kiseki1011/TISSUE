package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.AddReviewerCommand;
import com.tissue.issue.application.dto.request.AssignIssueCommand;
import com.tissue.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.issue.application.dto.request.UnsubscribeIssueCommand;
import com.tissue.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.event.IssueEventPublisher;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.policy.IssuePolicy;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueParticipantService implements IssueParticipantUseCase {

    private final IssueFinder issueFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssuePolicy issuePolicy;
    private final IssueAuthorizationService issueAuthService;
    private final ProjectAuthorizationService projectAuthService;
    private final CurrentMemberProvider currentMemberProvider;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void changeReporter(ChangeReporterCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        issueAuthService.requireParticipantManagePermission(
                cmd.workspaceKey(), cmd.projectKey(), cmd.issueKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        ProjectMember oldReporter = issue.getParticipants().getReporter();
        ProjectMember newReporter = projectMemberFinder.getBy(project, cmd.targetMemberId());
        ProjectMember actor = projectMemberFinder.getBy(project, actorMemberId);

        issue.changeReporter(newReporter);

        eventPublisher.publishReporterChanged(issue, oldReporter, newReporter, actor);
    }

    @Override
    public void assign(AssignIssueCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        issueAuthService.requireParticipantManagePermission(
                cmd.workspaceKey(), cmd.projectKey(), cmd.issueKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        ProjectMember assignee = projectMemberFinder.getBy(project, cmd.targetMemberId());
        ProjectMember actor = projectMemberFinder.getBy(project, actorMemberId);

        issue.assignTo(assignee);

        eventPublisher.publishAssigned(issue, assignee, actor);
    }

    @Override
    public void unassign(RemoveAssigneeCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        issueAuthService.requireParticipantManagePermission(
                cmd.workspaceKey(), cmd.projectKey(), cmd.issueKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        ProjectMember assignee = issue.getParticipants().getAssignee();
        if (assignee == null) {
            return;
        }
        ProjectMember actor = projectMemberFinder.getBy(project, actorMemberId);

        issue.unassign();

        eventPublisher.publishUnassigned(issue, assignee, actor);
    }

    @Override
    public void subscribe(SubscribeIssueCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireProjectViewer(cmd.workspaceKey(), cmd.projectKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        ProjectMember subscriber = projectMemberFinder.getBy(project, actorMemberId);

        issue.addSubscriber(subscriber);
    }

    @Override
    public void unsubscribe(UnsubscribeIssueCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireProjectViewer(cmd.workspaceKey(), cmd.projectKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        ProjectMember subscriber = projectMemberFinder.getBy(project, actorMemberId);

        issue.removeSubscriber(subscriber);
    }

    @Override
    public void addReviewer(AddReviewerCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        issueAuthService.requireReviewerManagePermission(
                cmd.workspaceKey(), cmd.projectKey(), cmd.issueKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        ProjectMember reviewer = projectMemberFinder.getBy(project, cmd.targetMemberId());
        ProjectMember actor = projectMemberFinder.getBy(project, actorMemberId);

        issuePolicy.ensureCanAddReviewer(issue);
        issue.addReviewer(reviewer);

        eventPublisher.publishReviewerAdded(issue, reviewer, actor);
    }

    @Override
    public void removeReviewer(RemoveReviewerCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        issueAuthService.requireReviewerManagePermission(
                cmd.workspaceKey(), cmd.projectKey(), cmd.issueKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        ProjectMember reviewer = projectMemberFinder.getBy(project, cmd.targetMemberId());
        ProjectMember actor = projectMemberFinder.getBy(project, actorMemberId);

        issue.removeReviewer(reviewer);

        eventPublisher.publishReviewerRemoved(issue, reviewer, actor);
    }
}
