package com.tissue.api.issue.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRelationRequest;
import com.tissue.api.issue.presentation.dto.response.CreateIssueRelationResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveIssueRelationResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationCommandService {

	private final IssueRepository issueRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional
	public CreateIssueRelationResponse createRelation(
		String workspaceCode,
		String sourceIssueKey,
		String targetIssueKey,
		Long requesterWorkspaceMemberId,
		CreateIssueRelationRequest request
	) {
		Issue sourceIssue = findIssue(workspaceCode, sourceIssueKey);
		Issue targetIssue = findIssue(workspaceCode, targetIssueKey);
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			sourceIssue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		IssueRelation.createRelation(sourceIssue, targetIssue, request.relationType());

		return CreateIssueRelationResponse.from(sourceIssue, targetIssue, request.relationType());
	}

	@Transactional
	public RemoveIssueRelationResponse removeRelation(
		String workspaceCode,
		String sourceIssueKey,
		String targetIssueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue sourceIssue = findIssue(workspaceCode, sourceIssueKey);
		Issue targetIssue = findIssue(workspaceCode, targetIssueKey);
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			sourceIssue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		IssueRelation.removeRelation(sourceIssue, targetIssue);

		return RemoveIssueRelationResponse.from(sourceIssue, targetIssue);
	}

	private Issue findIssue(String code, String issueKey) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, code)
			.orElseThrow(IssueNotFoundException::new);
	}

	private WorkspaceMember findWorkspaceMember(Long id) {
		return workspaceMemberRepository.findById(id)
			.orElseThrow(WorkspaceMemberNotFoundException::new);
	}
}
