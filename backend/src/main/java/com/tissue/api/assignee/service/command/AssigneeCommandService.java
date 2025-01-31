package com.tissue.api.assignee.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.assignee.presentation.dto.response.AddAssigneeResponse;
import com.tissue.api.assignee.presentation.dto.response.RemoveAssigneeResponse;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.query.IssueQueryService;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.query.WorkspaceMemberQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssigneeCommandService {

	private final IssueQueryService issueQueryService;
	private final WorkspaceMemberQueryService workspaceMemberQueryService;

	@Transactional
	public AddAssigneeResponse addAssignee(
		String workspaceCode,
		String issueKey,
		Long assigneeWorkspaceMemberId
	) {
		Issue issue = issueQueryService.findIssue(issueKey, workspaceCode);

		WorkspaceMember assignee = workspaceMemberQueryService.findWorkspaceMember(
			assigneeWorkspaceMemberId,
			workspaceCode
		);

		issue.addAssignee(assignee);

		return AddAssigneeResponse.from(assignee);
	}

	@Transactional
	public RemoveAssigneeResponse removeAssignee(
		String workspaceCode,
		String issueKey,
		Long assigneeWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueQueryService.findIssue(issueKey, workspaceCode);

		WorkspaceMember assignee = workspaceMemberQueryService.findWorkspaceMember(
			assigneeWorkspaceMemberId,
			workspaceCode
		);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(
			requesterWorkspaceMemberId,
			workspaceCode
		);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssignee(requesterWorkspaceMemberId);
		}

		issue.removeAssignee(assignee);

		return RemoveAssigneeResponse.from(assignee);
	}
}
