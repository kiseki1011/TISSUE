package com.tissue.api.assignee.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.assignee.domain.event.IssueAssignedEvent;
import com.tissue.api.assignee.domain.event.IssueUnassignedEvent;
import com.tissue.api.assignee.presentation.dto.response.AddAssigneeResponse;
import com.tissue.api.assignee.presentation.dto.response.RemoveAssigneeResponse;
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
	public AddAssigneeResponse addAssignee(
		String workspaceCode,
		String issueKey,
		Long assigneeWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember assignee = workspaceMemberReader.findWorkspaceMember(
			assigneeWorkspaceMemberId,
			workspaceCode
		);

		issue.addAssignee(assignee);

		eventPublisher.publishEvent(
			IssueAssignedEvent.createEvent(issue, assigneeWorkspaceMemberId, requesterWorkspaceMemberId)
		);

		return AddAssigneeResponse.from(assignee);
	}

	@Transactional
	public RemoveAssigneeResponse removeAssignee(
		String workspaceCode,
		String issueKey,
		Long assigneeWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember assignee = workspaceMemberReader.findWorkspaceMember(
			assigneeWorkspaceMemberId,
			workspaceCode
		);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(
			requesterWorkspaceMemberId,
			workspaceCode
		);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssignee(requesterWorkspaceMemberId);
		}

		issue.removeAssignee(assignee);

		eventPublisher.publishEvent(
			IssueUnassignedEvent.createEvent(issue, assigneeWorkspaceMemberId, requesterWorkspaceMemberId)
		);

		return RemoveAssigneeResponse.from(assignee);
	}
}
