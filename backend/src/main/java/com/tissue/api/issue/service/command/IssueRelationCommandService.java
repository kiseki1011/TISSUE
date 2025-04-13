package com.tissue.api.issue.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRelationRequest;
import com.tissue.api.issue.presentation.dto.response.CreateIssueRelationResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveIssueRelationResponse;
import com.tissue.api.issue.validator.checker.CircularDependencyChecker;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationCommandService {

	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final CircularDependencyChecker circularDependencyChecker;

	@Transactional
	public CreateIssueRelationResponse createRelation(
		String workspaceCode,
		String sourceIssueKey,
		String targetIssueKey,
		Long requesterWorkspaceMemberId,
		CreateIssueRelationRequest request
	) {
		Issue sourceIssue = issueReader.findIssue(sourceIssueKey, workspaceCode);
		Issue targetIssue = issueReader.findIssue(targetIssueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			sourceIssue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		if (request.relationType() == IssueRelationType.BLOCKS) {
			circularDependencyChecker.validateNoCircularDependency(sourceIssue, targetIssue);
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
		Issue sourceIssue = issueReader.findIssue(sourceIssueKey, workspaceCode);
		Issue targetIssue = issueReader.findIssue(targetIssueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			sourceIssue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		IssueRelation.removeRelation(sourceIssue, targetIssue);

		return RemoveIssueRelationResponse.from(sourceIssue, targetIssue);
	}
}
