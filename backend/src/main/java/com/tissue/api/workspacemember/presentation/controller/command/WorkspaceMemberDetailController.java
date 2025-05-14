package com.tissue.api.workspacemember.presentation.controller.command;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.security.authorization.interceptor.SelfOrRoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateDisplayNameRequest;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberCommandService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/members")
public class WorkspaceMemberDetailController {

	private final WorkspaceMemberCommandService workspaceMemberCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PatchMapping("/display-name")
	public ApiResponse<WorkspaceMemberResponse> updateDisplayName(
		@PathVariable String workspaceCode,
		@RequestBody @Valid UpdateDisplayNameRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		WorkspaceMemberResponse response = workspaceMemberCommandService.updateDisplayName(
			workspaceCode,
			loginMemberId,
			request
		);

		return ApiResponse.ok("Display name updated.", response);
	}

	@LoginRequired
	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@PatchMapping("/{memberId}/positions/{positionId}")
	public ApiResponse<WorkspaceMemberResponse> setPosition(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@PathVariable Long positionId,
		@ResolveLoginMember Long loginMemberId
	) {
		WorkspaceMemberResponse response = workspaceMemberCommandService.setPosition(
			workspaceCode,
			positionId,
			memberId,
			loginMemberId
		);

		return ApiResponse.ok("Position assigned to workspace member.", response);
	}

	@LoginRequired
	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@DeleteMapping("/{memberId}/positions/{positionId}")
	public ApiResponse<Void> removePosition(
		@PathVariable String workspaceCode,
		@PathVariable Long positionId,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId
	) {
		workspaceMemberCommandService.removePosition(
			workspaceCode,
			positionId,
			memberId,
			loginMemberId
		);

		return ApiResponse.okWithNoContent("Position removed from workspace member.");
	}

	@LoginRequired
	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@PatchMapping("/{memberId}/teams/{teamId}")
	public ApiResponse<WorkspaceMemberResponse> setTeam(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@PathVariable Long teamId,
		@ResolveLoginMember Long loginMemberId
	) {
		WorkspaceMemberResponse response = workspaceMemberCommandService.setTeam(
			workspaceCode,
			teamId,
			memberId,
			loginMemberId
		);

		return ApiResponse.ok("Team assigned to workspace member.", response);
	}

	@LoginRequired
	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@DeleteMapping("/{memberId}/teams/{teamId}")
	public ApiResponse<Void> removeTeam(
		@PathVariable String workspaceCode,
		@PathVariable Long teamId,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId
	) {
		workspaceMemberCommandService.removeTeam(
			workspaceCode,
			teamId,
			memberId,
			loginMemberId
		);

		return ApiResponse.okWithNoContent("Team removed from workspace member.");
	}
}
