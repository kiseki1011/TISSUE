package com.tissue.api.issue.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.AddAssigneeCommand;
import com.tissue.api.issue.application.dto.RemoveAssigneeCommand;
import com.tissue.api.issue.application.service.reader.IssueFinder;
import com.tissue.api.issue.domain.event.IssueAssignedEvent;
import com.tissue.api.issue.domain.event.IssueUnassignedEvent;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.presentation.controller.dto.response.IssueAssigneeResponse;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

// TODO: Move to IssueAssociateService
@Service
@RequiredArgsConstructor
public class IssueAssigneeCommandService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueAssigneeResponse addAssignee(
		String workspaceCode,
		String issueKey,
		AddAssigneeCommand command,
		Long requesterMemberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);

		WorkspaceMember assignee = workspaceMemberReader.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		issue.addAssignee(assignee);

		eventPublisher.publishEvent(
			IssueAssignedEvent.createEvent(issue, command.memberId(), requesterMemberId)
		);

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

		WorkspaceMember assignee = workspaceMemberReader.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(
			requesterMemberId,
			workspaceCode
		);

		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	issue.validateIsAssignee(requesterMemberId);
		// }

		issue.removeAssignee(assignee);

		eventPublisher.publishEvent(
			IssueUnassignedEvent.createEvent(issue, command.memberId(), requesterMemberId)
		);

		return IssueAssigneeResponse.from(assignee, issueKey);
	}
}
