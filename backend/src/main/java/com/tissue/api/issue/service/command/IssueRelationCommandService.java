package com.tissue.api.issue.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRelationRequest;
import com.tissue.api.issue.presentation.dto.response.CreateIssueRelationResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveIssueRelationResponse;
import com.tissue.api.issue.service.query.IssueQueryService;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.query.WorkspaceMemberQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationCommandService {

	private final IssueQueryService issueQueryService;
	private final WorkspaceMemberQueryService workspaceMemberQueryService;

	@Transactional
	public CreateIssueRelationResponse createRelation(
		String workspaceCode,
		String sourceIssueKey,
		String targetIssueKey,
		Long requesterWorkspaceMemberId,
		CreateIssueRelationRequest request
	) {
		Issue sourceIssue = issueQueryService.findIssue(sourceIssueKey, workspaceCode);
		Issue targetIssue = issueQueryService.findIssue(targetIssueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(requesterWorkspaceMemberId);

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
		Issue sourceIssue = issueQueryService.findIssue(sourceIssueKey, workspaceCode);
		Issue targetIssue = issueQueryService.findIssue(targetIssueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			sourceIssue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		IssueRelation.removeRelation(sourceIssue, targetIssue);

		return RemoveIssueRelationResponse.from(sourceIssue, targetIssue);
	}
}
