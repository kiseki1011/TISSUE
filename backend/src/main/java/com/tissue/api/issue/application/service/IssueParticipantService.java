package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueParticipantService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;

	@Transactional
	public IssueResponse assignTo(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember assignee = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.assignTo(assignee);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse unassign(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		issue.unassign();

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse subscribe(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember subscriber = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.addSubscriber(subscriber);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse unsubscribe(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember subscriber = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.removeSubscriber(subscriber);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse addReviewer(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember reviewer = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.addReviewer(reviewer);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse removeReviewer(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember reviewer = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.removeReviewer(reviewer);

		return IssueResponse.from(issue);
	}
}
