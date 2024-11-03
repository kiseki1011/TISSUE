package com.uranus.taskmanager.api.workspace.controller;

import org.springframework.data.domain.Pageable;
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

import com.uranus.taskmanager.api.authentication.LoginMember;
import com.uranus.taskmanager.api.authentication.LoginRequired;
import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceContentUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspacePasswordUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceContentUpdateResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceCreateResponse;
import com.uranus.taskmanager.api.workspace.service.WorkspaceCommandService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceCreateService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceQueryService;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.authorization.RoleRequired;

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
	public ApiResponse<WorkspaceCreateResponse> createWorkspace(
		@LoginMember LoginMemberDto loginMember,
		@RequestBody @Valid WorkspaceCreateRequest request) {

		WorkspaceCreateResponse response = workspaceCreateService.createWorkspace(request, loginMember);
		return ApiResponse.created("Workspace Created", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@DeleteMapping("/{code}")
	public ApiResponse<String> deleteWorkspace(@PathVariable String code,
		@RequestBody WorkspaceDeleteRequest request) {

		workspaceCommandService.deleteWorkspace(request, code);
		return ApiResponse.ok("Workspace Deleted", code);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}")
	public ApiResponse<WorkspaceContentUpdateResponse> updateWorkspaceContent(@PathVariable String code,
		@RequestBody @Valid WorkspaceContentUpdateRequest request) {

		WorkspaceContentUpdateResponse response = workspaceCommandService.updateWorkspaceContent(request, code);
		return ApiResponse.ok("Workspace Title and Description Updated", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@PutMapping("/{code}/password")
	public ApiResponse<String> updateWorkspacePassword(@PathVariable String code,
		@RequestBody @Valid WorkspacePasswordUpdateRequest request) {

		workspaceCommandService.updateWorkspacePassword(request, code);
		return ApiResponse.okWithNoContent("Workspace Password Updated");
	}

	@LoginRequired
	@GetMapping("/{code}")
	public ApiResponse<WorkspaceDetail> getWorkspaceDetail(@PathVariable String code,
		@LoginMember LoginMemberDto loginMember) {

		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(code, loginMember);
		return ApiResponse.ok("Workspace Found", response);
	}

	@LoginRequired
	@GetMapping
	public ApiResponse<MyWorkspacesResponse> getMyWorkspaces(@LoginMember LoginMemberDto loginMember,
		Pageable pageable) {

		MyWorkspacesResponse response = workspaceQueryService.getMyWorkspaces(loginMember, pageable);
		return ApiResponse.ok("Currently joined Workspaces Found", response);
	}
}
