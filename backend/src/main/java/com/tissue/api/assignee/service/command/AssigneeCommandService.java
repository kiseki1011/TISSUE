package com.tissue.api.assignee.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.assignee.presentation.dto.response.AddAssigneeResponse;
import com.tissue.api.assignee.presentation.dto.response.RemoveAssigneeResponse;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssigneeCommandService {

	private final IssueRepository issueRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional
	public AddAssigneeResponse addAssignee(
		String workspaceCode,
		String issueKey,
		Long assigneeWorkspaceMemberId
	) {
		Issue issue = findIssue(issueKey, workspaceCode);
		WorkspaceMember assignee = findWorkspaceMember(assigneeWorkspaceMemberId, workspaceCode);

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
		Issue issue = findIssue(issueKey, workspaceCode);
		WorkspaceMember assignee = findWorkspaceMember(assigneeWorkspaceMemberId, workspaceCode);
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId, workspaceCode);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssignee(requesterWorkspaceMemberId);
		}

		issue.removeAssignee(assignee);

		return RemoveAssigneeResponse.from(assignee);
	}

	private Issue findIssue(String issueKey, String code) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, code)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, code));
	}

	private WorkspaceMember findWorkspaceMember(Long id, String code) {
		return workspaceMemberRepository.findByIdAndWorkspaceCode(id, code)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(id, code));
	}

}
