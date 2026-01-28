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
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.policy.IssuePolicy;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
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
    private final IssueEventPublisher eventPublisher;

    @Override
    public void changeReporter(ChangeReporterCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        Project project = projectFinder.getModifiableBy(actor.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireParticipantManagePermission(issue, actor);

        ProjectMember oldReporter = issue.getParticipants().getReporter();

        ProjectMember newReporter = projectMemberFinder.getActive(project, cmd.targetMemberId());
        issue.changeReporter(newReporter);

        eventPublisher.publishReporterChanged(issue, oldReporter, newReporter, actor);
    }

    @Override
    public void assign(AssignIssueCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        Project project = projectFinder.getModifiableBy(actor.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireParticipantManagePermission(issue, actor);

        ProjectMember assignee = projectMemberFinder.getIncludingSoftDeleted(project, cmd.targetMemberId());
        issue.assignTo(assignee);

        eventPublisher.publishAssigned(issue, assignee, actor);
    }

    @Override
    public void unassign(RemoveAssigneeCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        Project project = projectFinder.getModifiableBy(actor.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireParticipantManagePermission(issue, actor);

        ProjectMember assignee = issue.getParticipants().getAssignee();
        if (assignee == null) {
            return;
        }

        issue.unassign();

        eventPublisher.publishUnassigned(issue, assignee, actor);
    }

    @Override
    public void subscribe(SubscribeIssueCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        Project project = projectFinder.getModifiableBy(actor.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        projectAuthService.requireProjectViewer(actor);

        ProjectMember subscriber = projectMemberFinder.getActive(project, actor.memberId());
        issue.addSubscriber(subscriber);
    }

    @Override
    public void unsubscribe(UnsubscribeIssueCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        Project project = projectFinder.getModifiableBy(actor.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        projectAuthService.requireProjectViewer(actor);

        ProjectMember subscriber = projectMemberFinder.getActive(project, actor.memberId());
        issue.removeSubscriber(subscriber);
    }

    @Override
    public void addReviewer(AddReviewerCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        Project project = projectFinder.getModifiableBy(actor.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireReviewerManagePermission(issue, actor);
        issuePolicy.ensureCanAddReviewer(issue);

        ProjectMember reviewer = projectMemberFinder.getActive(project, cmd.targetMemberId());
        issue.addReviewer(reviewer);

        eventPublisher.publishReviewerAdded(issue, reviewer, actor);
    }

    @Override
    public void removeReviewer(RemoveReviewerCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        Project project = projectFinder.getModifiableBy(actor.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireReviewerManagePermission(issue, actor);

        ProjectMember reviewer = projectMemberFinder.getIncludingSoftDeleted(project, cmd.targetMemberId());
        issue.removeReviewer(reviewer);

        eventPublisher.publishReviewerRemoved(issue, reviewer, actor);
    }
}
