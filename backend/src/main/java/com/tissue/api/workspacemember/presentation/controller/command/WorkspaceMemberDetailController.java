package com.tissue.api.workspacemember.presentation.controller.command;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.security.authorization.interceptor.SelfOrRoleRequired;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberCommandService;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateDisplayNameRequest;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/members")
public class WorkspaceMemberDetailController {

	private final WorkspaceMemberCommandService workspaceMemberCommandService;

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PatchMapping("/display-name")
	public ApiResponse<WorkspaceMemberResponse> updateDisplayName(
		@PathVariable String workspaceCode,
		@RequestBody @Valid UpdateDisplayNameRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberCommandService.updateDisplayName(
			workspaceCode,
			userDetails.getMemberId(),
			request
		);

		return ApiResponse.ok("Display name updated.", response);
	}

	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@PatchMapping("/{memberId}/positions/{positionId}")
	public ApiResponse<WorkspaceMemberResponse> setPosition(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@PathVariable Long positionId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberCommandService.setPosition(
			workspaceCode,
			positionId,
			memberId,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Position assigned to workspace member.", response);
	}

	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@DeleteMapping("/{memberId}/positions/{positionId}")
	public ApiResponse<Void> removePosition(
		@PathVariable String workspaceCode,
		@PathVariable Long positionId,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberCommandService.removePosition(
			workspaceCode,
			positionId,
			memberId,
			userDetails.getMemberId()
		);

		return ApiResponse.okWithNoContent("Position removed from workspace member.");
	}

	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@PatchMapping("/{memberId}/teams/{teamId}")
	public ApiResponse<WorkspaceMemberResponse> setTeam(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@PathVariable Long teamId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberCommandService.setTeam(
			workspaceCode,
			teamId,
			memberId,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Team assigned to workspace member.", response);
	}

	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@DeleteMapping("/{memberId}/teams/{teamId}")
	public ApiResponse<Void> removeTeam(
		@PathVariable String workspaceCode,
		@PathVariable Long teamId,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberCommandService.removeTeam(
			workspaceCode,
			teamId,
			memberId,
			userDetails.getMemberId()
		);

		return ApiResponse.okWithNoContent("Team removed from workspace member.");
	}
}
