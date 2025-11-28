package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;

import com.tissue.api.issue.application.dto.request.AddReviewerCommand;
import com.tissue.api.issue.application.dto.request.AssignIssueCommand;
import com.tissue.api.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.api.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.api.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.api.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.api.issue.application.dto.request.UnsubscribeIssueCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;
import com.tissue.api.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.policy.IssuePolicy;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.application.service.finder.ProjectMemberFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.ProjectMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueParticipantService implements IssueParticipantUseCase {

	private final IssueFinder issueFinder;
	private final ProjectFinder projectFinder;
	private final ProjectMemberFinder projectMemberFinder;
	private final IssuePolicy issuePolicy;

	@Override
	public IssueCommandResult changeReporter(ChangeReporterCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		ProjectMember target = projectMemberFinder.findBy(project, cmd.memberId());

		issue.changeReporter(target);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult assign(AssignIssueCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		ProjectMember assignee = projectMemberFinder.findBy(project, cmd.memberId());

		issue.assignTo(assignee);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult unassign(RemoveAssigneeCommand cmd) {

		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		issue.unassign();

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult subscribe(SubscribeIssueCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		ProjectMember subscriber = projectMemberFinder.findBy(project, cmd.memberId());

		issue.addSubscriber(subscriber);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult unsubscribe(UnsubscribeIssueCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		ProjectMember subscriber = projectMemberFinder.findBy(project, cmd.memberId());

		issue.removeSubscriber(subscriber);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult addReviewer(AddReviewerCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		ProjectMember reviewer = projectMemberFinder.findBy(project, cmd.memberId());

		issuePolicy.ensureCanAddReviewer(issue);
		issue.addReviewer(reviewer);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult removeReviewer(RemoveReviewerCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		ProjectMember reviewer = projectMemberFinder.findBy(project, cmd.memberId());

		issue.removeReviewer(reviewer);

		return IssueCommandResult.from(issue);
	}
}
