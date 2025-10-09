package com.tissue.api.issue.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.dto.RemoveParentIssueCommand;
import com.tissue.api.issue.application.service.IssueService;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues")
public class IssueController {

	private final IssueService issueService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ResponseEntity<ApiResponse<IssueResponse>> createIssue(
		@PathVariable String workspaceKey,
		@RequestBody @Valid CreateIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.createIssue(request.toCommand(workspaceKey));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Issue created.", response));
	}

	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{issueKey}")
	// public ApiResponse<IssueResponse> updateIssue(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String issueKey,
	// 	@RequestBody @Valid UpdateIssueRequest request,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	IssueResponse response = issueService.updateIssue(request.toCommand(workspaceCode, issueKey));
	// 	return ApiResponse.ok("Issue updated.", response);
	// }

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> assignParentIssue(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AssignParentIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.assignParentIssue(request.toCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Parent issue assigned.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> removeParentIssue(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.removeParentIssue(new RemoveParentIssueCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Parent issue removed.", response);
	}

	// TODO: Progress Issue(Update Issue's WorkflowStep)
	//  Should this API be placed inside IssueController or WorkflowController?

	// TODO: Soft delete Issue

	// TODO(Later):
	//  - Clone Issue
	//  - Move(or clone) Issue to different Workspace
}
