package com.tissue.api.workspacemember.presentation.controller;

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
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateNicknameRequest;
import com.tissue.api.workspacemember.presentation.dto.response.AssignPositionResponse;
import com.tissue.api.workspacemember.presentation.dto.response.AssignTeamResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateNicknameResponse;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberCommandService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/members")
public class WorkspaceMemberDetailController {

	private final WorkspaceMemberCommandService workspaceMemberCommandService;

	/*
	 * Todo
	 *  - <br>
	 *  - 구현 예정
	 *  - setNicknameSchema: 워크스페이스의 별칭 스키마 정하기 (OWNER)
	 *    - 예시: [멤버의 소속 부서/팀][멤버의 포지션]멤버의 이름 -> 이렇게 정하는 것이 가능, 중복되는 경우 숫자 붙이기
	 *    - 예시: [SearchTeam][BACKEND-DEV]HongGilDong
	 */
	@LoginRequired
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PatchMapping("/nickname")
	public ApiResponse<UpdateNicknameResponse> updateMyNickname(
		@PathVariable String workspaceCode,
		@RequestBody @Valid UpdateNicknameRequest request,
		@ResolveLoginMember Long memberId
	) {
		UpdateNicknameResponse response = workspaceMemberCommandService.updateNickname(
			workspaceCode,
			memberId,
			request
		);

		return ApiResponse.ok("Nickname updated.", response);
	}

	@LoginRequired
	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@PatchMapping("/{memberId}/positions/{positionId}")
	public ApiResponse<AssignPositionResponse> setPosition(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@PathVariable Long positionId,
		@ResolveLoginMember Long loginMemberId
	) {
		AssignPositionResponse response = workspaceMemberCommandService.setPosition(
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
	public ApiResponse<Void> clearPosition(
		@PathVariable String workspaceCode,
		@PathVariable Long positionId,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId
	) {
		workspaceMemberCommandService.clearPosition(
			workspaceCode,
			positionId,
			memberId,
			loginMemberId
		);

		return ApiResponse.okWithNoContent("Position cleared from workspace member.");
	}

	@LoginRequired
	@SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@PatchMapping("/{memberId}/teams/{teamId}")
	public ApiResponse<AssignTeamResponse> setTeam(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@PathVariable Long teamId,
		@ResolveLoginMember Long loginMemberId
	) {
		AssignTeamResponse response = workspaceMemberCommandService.assignTeam(
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
	public ApiResponse<Void> clearTeam(
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

		return ApiResponse.okWithNoContent("Team cleared from workspace member.");
	}
}
