package com.tissue.api.issue.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.AddAssigneeCommand;
import com.tissue.api.issue.application.dto.AddWatcherCommand;
import com.tissue.api.issue.application.dto.RemoveAssigneeCommand;
import com.tissue.api.issue.application.dto.RemoveWatcherCommand;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.presentation.dto.response.IssueAssigneeResponse;
import com.tissue.api.issue.presentation.dto.response.IssueCollaboratorResponse;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

// TODO: Move to IssueAssociateService
@Service
@RequiredArgsConstructor
public class IssueCollaboratorService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueAssigneeResponse addAssignee(
		String workspaceCode,
		String issueKey,
		AddAssigneeCommand command,
		Long requesterMemberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);

		WorkspaceMember assignee = workspaceMemberFinder.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		issue.addAssignee(assignee);

		// eventPublisher.publishEvent(
		// 	IssueAssignedEvent.createEvent(issue, command.memberId(), requesterMemberId)
		// );

		return IssueAssigneeResponse.from(assignee, issueKey);
	}

	@Transactional
	public IssueAssigneeResponse removeAssignee(
		String workspaceCode,
		String issueKey,
		RemoveAssigneeCommand command,
		Long requesterMemberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);

		WorkspaceMember assignee = workspaceMemberFinder.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(
			requesterMemberId,
			workspaceCode
		);

		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	issue.validateIsAssignee(requesterMemberId);
		// }

		issue.removeAssignee(assignee);

		// eventPublisher.publishEvent(
		// 	IssueUnassignedEvent.createEvent(issue, command.memberId(), requesterMemberId)
		// );

		return IssueAssigneeResponse.from(assignee, issueKey);
	}

	@Transactional
	public IssueCollaboratorResponse watchIssue(
		AddWatcherCommand cmd
	) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceCode());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(
			cmd.memberId(),
			cmd.workspaceCode()
		);

		issue.addWatcher(workspaceMember);

		return IssueCollaboratorResponse.from(issue, cmd.memberId());
	}

	@Transactional
	public IssueCollaboratorResponse unwatchIssue(
		RemoveWatcherCommand cmd
	) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceCode());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(
			cmd.memberId(),
			cmd.workspaceCode()
		);

		issue.removeWatcher(workspaceMember);

		return IssueCollaboratorResponse.from(issue, cmd.memberId());
	}
}
