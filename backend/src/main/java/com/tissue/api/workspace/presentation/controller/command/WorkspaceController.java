package com.tissue.api.workspace.presentation.controller.command;

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
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspace.application.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateService;
import com.tissue.api.workspace.application.service.query.WorkspaceQueryService;
import com.tissue.api.workspace.domain.service.WorkspaceAuthenticationService;
import com.tissue.api.workspace.presentation.dto.WorkspaceDetail;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.request.DeleteWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateIssueKeyRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.tissue.api.workspace.presentation.dto.response.WorkspaceResponse;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

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
	private final WorkspaceAuthenticationService workspaceAuthenticationService;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<WorkspaceResponse> createWorkspace(
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid CreateWorkspaceRequest request
	) {
		WorkspaceResponse response = workspaceCreateService.createWorkspace(
			request,
			userDetails.getMemberId()
		);

		return ApiResponse.created("Workspace created.", response);
	}

	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{workspaceCode}/info")
	public ApiResponse<WorkspaceResponse> updateWorkspaceInfo(
		@PathVariable String workspaceCode,
		@RequestBody @Valid UpdateWorkspaceInfoRequest request
	) {
		WorkspaceResponse response = workspaceCommandService.updateWorkspaceInfo(
			request,
			workspaceCode
		);

		return ApiResponse.ok("Workspace info updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}/password")
	public ApiResponse<WorkspaceResponse> updateWorkspacePassword(
		@PathVariable String workspaceCode,
		@RequestBody @Valid UpdateWorkspacePasswordRequest request
	) {
		workspaceAuthenticationService.authenticate(request.originalPassword(), workspaceCode);
		WorkspaceResponse response = workspaceCommandService.updateWorkspacePassword(request, workspaceCode);

		return ApiResponse.ok("Workspace password updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.OWNER)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{workspaceCode}")
	public ApiResponse<Void> deleteWorkspace(
		@PathVariable String workspaceCode,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody DeleteWorkspaceRequest request
	) {
		workspaceAuthenticationService.authenticate(request.password(), workspaceCode);
		workspaceCommandService.deleteWorkspace(workspaceCode, userDetails.getMemberId());

		return ApiResponse.okWithNoContent("Workspace deleted.");
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{code}")
	public ApiResponse<WorkspaceDetail> getWorkspaceDetail(
		@PathVariable String code
	) {
		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(code);

		return ApiResponse.ok("Workspace found.", response);
	}

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
