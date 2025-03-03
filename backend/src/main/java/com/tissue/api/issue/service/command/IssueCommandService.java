package com.tissue.api.issue.service.command;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.AssignParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.UpdateIssueStatusResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.issue.presentation.dto.response.delete.DeleteIssueResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateIssueResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.query.WorkspaceMemberQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommandService {

	private final IssueReader issueReader;
	private final WorkspaceQueryService workspaceQueryService;
	private final WorkspaceMemberQueryService workspaceMemberQueryService;
	private final IssueRepository issueRepository;

	@Transactional
	public CreateIssueResponse createIssue(
		String workspaceCode,
		CreateIssueRequest request
	) {
		Workspace workspace = workspaceQueryService.findWorkspace(workspaceCode);

		Issue issue = request.toIssue(workspace);

		Issue savedIssue = issueRepository.save(issue);
		return CreateIssueResponse.from(savedIssue);
	}

	@Transactional
	public UpdateIssueResponse updateIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		UpdateIssueRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(requesterWorkspaceMemberId);

		issue.validateIssueTypeMatch(request.getType());

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		request.update(issue);

		return UpdateIssueResponse.from(issue);
	}

	@Transactional
	public UpdateIssueStatusResponse updateIssueStatus(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		UpdateIssueStatusRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		issue.updateStatus(request.status());

		return UpdateIssueStatusResponse.from(issue);
	}

	@Transactional
	public AssignParentIssueResponse assignParentIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		AssignParentIssueRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		Issue parentIssue = issueReader.findIssue(request.parentIssueKey(), workspaceCode);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		issue.updateParentIssue(parentIssue);

		return AssignParentIssueResponse.from(issue);
	}

	@Transactional
	public RemoveParentIssueResponse removeParentIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// sub-task의 부모 제거 방지용 로직
		issue.validateCanRemoveParent();
		issue.removeParentRelationship();

		return RemoveParentIssueResponse.from(issue);
	}

	@Transactional
	public DeleteIssueResponse deleteIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		if (issue.getType() != IssueType.EPIC) {
			issue.validateHasChildIssues();
		}

		Long issueId = issue.getId();

		issueRepository.delete(issue);

		return DeleteIssueResponse.from(issueId, issueKey, LocalDateTime.now());
	}
}
