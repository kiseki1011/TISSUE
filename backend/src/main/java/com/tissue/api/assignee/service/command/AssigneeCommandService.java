package com.tissue.api.assignee.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.assignee.domain.event.IssueAssignedEvent;
import com.tissue.api.assignee.domain.event.IssueUnassignedEvent;
import com.tissue.api.assignee.presentation.dto.response.IssueAssigneeResponse;
import com.tissue.api.assignee.service.dto.AddAssigneeCommand;
import com.tissue.api.assignee.service.dto.RemoveAssigneeCommand;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssigneeCommandService {

	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueAssigneeResponse addAssignee(
		String workspaceCode,
		String issueKey,
		AddAssigneeCommand command,
		Long requesterMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

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
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember assignee = workspaceMemberReader.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(
			requesterMemberId,
			workspaceCode
		);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssignee(requesterMemberId);
		}

		issue.removeAssignee(assignee);

		eventPublisher.publishEvent(
			IssueUnassignedEvent.createEvent(issue, command.memberId(), requesterMemberId)
		);

		return IssueAssigneeResponse.from(assignee, issueKey);
	}
}
