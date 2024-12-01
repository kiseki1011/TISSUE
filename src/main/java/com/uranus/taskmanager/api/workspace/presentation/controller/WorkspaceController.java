package com.uranus.taskmanager.api.workspace.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authentication.resolver.ResolveLoginMember;
import com.uranus.taskmanager.api.security.authorization.interceptor.RoleRequired;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.DeleteWorkspaceRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.UpdateWorkspaceRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.UpdateWorkspaceResponse;
import com.uranus.taskmanager.api.workspace.service.command.WorkspaceCommandService;
import com.uranus.taskmanager.api.workspace.service.command.create.WorkspaceCreateService;
import com.uranus.taskmanager.api.workspace.service.query.WorkspaceQueryService;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

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
		return ApiResponse.created("Workspace Created", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.OWNER)
	@DeleteMapping("/{code}")
	public ApiResponse<String> deleteWorkspace(
		@PathVariable String code,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody DeleteWorkspaceRequest request
	) {

		workspaceCommandService.deleteWorkspace(
			request,
			code,
			loginMemberId
		);
		return ApiResponse.ok("Workspace Deleted", code);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.MANAGER)
	@PatchMapping("/{code}")
	public ApiResponse<UpdateWorkspaceResponse> updateWorkspaceContent(
		@PathVariable String code,
		@RequestBody @Valid UpdateWorkspaceRequest request
	) {

		UpdateWorkspaceResponse response = workspaceCommandService.updateWorkspaceContent(
			request,
			code
		);
		return ApiResponse.ok("Workspace Title and Description Updated", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.MANAGER)
	@PutMapping("/{code}/password")
	public ApiResponse<String> updateWorkspacePassword(
		@PathVariable String code,
		@RequestBody @Valid UpdateWorkspacePasswordRequest request
	) {

		workspaceCommandService.updateWorkspacePassword(
			request,
			code
		);
		return ApiResponse.okWithNoContent("Workspace Password Updated");
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.VIEWER)
	@GetMapping("/{code}")
	public ApiResponse<WorkspaceDetail> getWorkspaceDetail(@PathVariable String code) {

		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(code);
		return ApiResponse.ok("Workspace Found", response);
	}

}
