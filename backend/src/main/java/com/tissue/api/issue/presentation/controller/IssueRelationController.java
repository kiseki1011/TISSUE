package com.tissue.api.issue.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRelationRequest;
import com.tissue.api.issue.presentation.dto.response.CreateIssueRelationResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveIssueRelationResponse;
import com.tissue.api.issue.service.command.IssueRelationCommandService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{code}/issues/{issueKey}/relations")
public class IssueRelationController {

	private final IssueRelationCommandService issueRelationCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{targetIssueKey}")
	public ApiResponse<CreateIssueRelationResponse> createRelation(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable String targetIssueKey,
		@CurrentWorkspaceMember Long currentWorkspaceMemberId,
		@RequestBody @Valid CreateIssueRelationRequest request
	) {
		CreateIssueRelationResponse response = issueRelationCommandService.createRelation(
			code,
			issueKey,
			targetIssueKey,
			currentWorkspaceMemberId,
			request
		);

		return ApiResponse.ok("Issue relation created.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{targetIssueKey}")
	public ApiResponse<RemoveIssueRelationResponse> removeRelation(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable String targetIssueKey,
		@CurrentWorkspaceMember Long currentWorkspaceMemberId
	) {
		RemoveIssueRelationResponse response = issueRelationCommandService.removeRelation(
			code,
			issueKey,
			targetIssueKey,
			currentWorkspaceMemberId
		);

		return ApiResponse.ok("Issue relation removed.", response);
	}
}
