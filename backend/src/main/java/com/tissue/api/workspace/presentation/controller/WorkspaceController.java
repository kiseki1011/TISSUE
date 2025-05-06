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
import com.tissue.api.workspace.presentation.dto.response.WorkspaceResponse;
import com.tissue.api.workspace.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.service.command.create.WorkspaceCreateService;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;
import com.tissue.api.workspace.validator.WorkspaceValidator;
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
	private final WorkspaceValidator workspaceValidator;

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
	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}/info")
	public ApiResponse<WorkspaceResponse> updateWorkspaceInfo(
		@PathVariable String code,
		@RequestBody @Valid UpdateWorkspaceInfoRequest request
	) {
		WorkspaceResponse response = workspaceCommandService.updateWorkspaceInfo(
			request,
			code
		);

		return ApiResponse.ok("Workspace info updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}/password")
	public ApiResponse<WorkspaceResponse> updateWorkspacePassword(
		@PathVariable String code,
		@RequestBody @Valid UpdateWorkspacePasswordRequest request
	) {
		workspaceValidator.validateWorkspacePassword(request.originalPassword(), code);
		WorkspaceResponse response = workspaceCommandService.updateWorkspacePassword(request, code);

		return ApiResponse.ok("Workspace password updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.OWNER)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{code}")
	public ApiResponse<Void> deleteWorkspace(
		@PathVariable String code,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody DeleteWorkspaceRequest request
	) {
		workspaceValidator.validateWorkspacePassword(request.password(), code);
		workspaceCommandService.deleteWorkspace(code, loginMemberId);

		return ApiResponse.okWithNoContent("Workspace deleted.");
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{code}")
	public ApiResponse<WorkspaceDetail> getWorkspaceDetail(
		@PathVariable String code
	) {
		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(code);

		return ApiResponse.ok("Workspace found.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}/key")
	public ApiResponse<WorkspaceResponse> updateIssueKey(
		@PathVariable String code,
		@RequestBody @Valid UpdateIssueKeyRequest request
	) {
		WorkspaceResponse response = workspaceCommandService.updateIssueKeyPrefix(
			code,
			request
		);

		return ApiResponse.ok("Issue key prefix updated.", response);
	}
}
