package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.request.AddReviewerCommand;
import com.tissue.api.issue.application.dto.request.AssignIssueCommand;
import com.tissue.api.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.api.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.api.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.api.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.api.issue.application.dto.request.UnsubscribeIssueCommand;
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
	@Transactional
	public void changeReporter(ChangeReporterCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		ProjectMember target = projectMemberFinder.findBy(project, cmd.memberId());

		issue.changeReporter(target);
	}

	@Override
	@Transactional
	public void assign(AssignIssueCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		ProjectMember assignee = projectMemberFinder.findBy(project, cmd.memberId());

		issue.assignTo(assignee);
	}

	@Override
	@Transactional
	public void unassign(RemoveAssigneeCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		issue.unassign();
	}

	@Override
	@Transactional
	public void subscribe(SubscribeIssueCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		ProjectMember subscriber = projectMemberFinder.findBy(project, cmd.memberId());

		issue.addSubscriber(subscriber);
	}

	@Override
	@Transactional
	public void unsubscribe(UnsubscribeIssueCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		ProjectMember subscriber = projectMemberFinder.findBy(project, cmd.memberId());

		issue.removeSubscriber(subscriber);
	}

	@Override
	@Transactional
	public void addReviewer(AddReviewerCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		ProjectMember reviewer = projectMemberFinder.findBy(project, cmd.memberId());

		issuePolicy.ensureCanAddReviewer(issue);
		issue.addReviewer(reviewer);
	}

	@Override
	@Transactional
	public void removeReviewer(RemoveReviewerCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		ProjectMember reviewer = projectMemberFinder.findBy(project, cmd.memberId());

		issue.removeReviewer(reviewer);
	}
}
