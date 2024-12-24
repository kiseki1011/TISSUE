package com.tissue.api.workspace.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspace.presentation.dto.WorkspaceDetail;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.request.DeleteWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateIssueKeyRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.api.workspace.presentation.dto.response.DeleteWorkspaceResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateIssueKeyResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateWorkspaceInfoResponse;
import com.tissue.api.workspace.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.service.command.create.WorkspaceCreateService;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

	private final WorkspaceCreateService workspaceCreateService;
	private final WorkspaceCommandService workspaceCommandService;
	private final WorkspaceQueryService workspaceQueryService;

	@LoginRequired
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<CreateWorkspaceResponse> createWorkspace(
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid CreateWorkspaceRequest request
	) {
		CreateWorkspaceResponse response = workspaceCreateService.createWorkspace(
			request,
			loginMemberId
		);
		return ApiResponse.created("Workspace created.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}/info")
	public ApiResponse<UpdateWorkspaceInfoResponse> updateWorkspaceInfo(
		@PathVariable String code,
		@RequestBody @Valid UpdateWorkspaceInfoRequest request
	) {
		UpdateWorkspaceInfoResponse response = workspaceCommandService.updateWorkspaceInfo(
			request,
			code
		);
		return ApiResponse.ok("Workspace info updated.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}/password")
	public ApiResponse<Void> updateWorkspacePassword(
		@PathVariable String code,
		@RequestBody @Valid UpdateWorkspacePasswordRequest request
	) {
		workspaceCommandService.updateWorkspacePassword(
			request,
			code
		);
		return ApiResponse.okWithNoContent("Workspace password updated.");
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.OWNER)
	@DeleteMapping("/{code}")
	public ApiResponse<DeleteWorkspaceResponse> deleteWorkspace(
		@PathVariable String code,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody DeleteWorkspaceRequest request
	) {
		DeleteWorkspaceResponse response = workspaceCommandService.deleteWorkspace(
			request,
			code,
			loginMemberId
		);
		return ApiResponse.ok("Workspace deleted.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.VIEWER)
	@GetMapping("/{code}")
	public ApiResponse<WorkspaceDetail> getWorkspaceDetail(
		@PathVariable String code
	) {
		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(code);
		return ApiResponse.ok("Workspace found.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}/key")
	public ApiResponse<UpdateIssueKeyResponse> updateIssueKey(
		@PathVariable String code,
		@RequestBody @Valid UpdateIssueKeyRequest request
	) {
		UpdateIssueKeyResponse response = workspaceCommandService.updateIssueKey(
			code,
			request
		);
		return ApiResponse.ok("Issue key prefix updated.", response);
	}

	// @LoginRequired
	// @RoleRequired(roles = WorkspaceRole.VIEWER)
	// @GetMapping("/{code}/switch")
	// public ApiResponse<Void> switchWorkspace(
	// 	@PathVariable String code,
	// 	@ResolveLoginMember Long loginMemberId,
	// 	HttpSession session
	// ) {
	// 	Long workspaceMemberId = workspaceQueryService.getWorkspaceMemberId(code, loginMemberId);
	//
	// 	session.setAttribute(SessionAttributes.CURRENT_WORKSPACE_CODE, code);
	// 	session.setAttribute(SessionAttributes.CURRENT_WORKSPACE_MEMBER_ID, workspaceMemberId);
	//
	// 	return ApiResponse.okWithNoContent("Workspace switched to \"" + code + "\".");
	// }

}
