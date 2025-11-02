package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.response.IssueCommandResult;
import com.tissue.api.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.application.service.policy.IssuePolicy;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workspacemember.application.finder.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueParticipantService implements IssueParticipantUseCase {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final IssuePolicy issuePolicy;

	@Override
	public IssueCommandResult changeReporter(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.changeReporter(target);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult assignTo(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember assignee = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.assignTo(assignee);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult unassign(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		issue.unassign();

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult subscribe(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember subscriber = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.addSubscriber(subscriber);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult unsubscribe(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember subscriber = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.removeSubscriber(subscriber);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult addReviewer(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember reviewer = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issuePolicy.ensureCanAddReviewer(issue);
		issue.addReviewer(reviewer);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult removeReviewer(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember reviewer = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.removeReviewer(reviewer);

		return IssueCommandResult.from(issue);
	}
}
