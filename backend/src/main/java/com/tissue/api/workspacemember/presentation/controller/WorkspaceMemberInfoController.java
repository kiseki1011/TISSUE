package com.tissue.api.workspacemember.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateNicknameRequest;
import com.tissue.api.workspacemember.presentation.dto.response.AssignPositionResponse;
import com.tissue.api.workspacemember.presentation.dto.response.AssignTeamResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateNicknameResponse;
import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMember;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberCommandService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/members")
public class WorkspaceMemberInfoController {

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
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@PatchMapping("/nickname")
	public ApiResponse<UpdateNicknameResponse> updateMyNickname(
		@CurrentWorkspaceMember Long workspaceMemberId,
		@RequestBody @Valid UpdateNicknameRequest request
	) {
		UpdateNicknameResponse response = workspaceMemberCommandService.updateNickname(
			workspaceMemberId,
			request
		);

		return ApiResponse.ok("Nickname updated.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@PatchMapping("/positions/{positionId}")
	public ApiResponse<AssignPositionResponse> assignMyPosition(
		@PathVariable String code,
		@PathVariable Long positionId,
		@CurrentWorkspaceMember Long workspaceMemberId
	) {
		AssignPositionResponse response = workspaceMemberCommandService.assignPosition(
			code,
			positionId,
			workspaceMemberId
		);

		return ApiResponse.ok("Position assigned to workspace member.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@DeleteMapping("/positions/{positionId}")
	public ApiResponse<Void> removeMyPosition(
		@PathVariable String code,
		@PathVariable Long positionId,
		@CurrentWorkspaceMember Long workspaceMemberId
	) {
		workspaceMemberCommandService.removePosition(
			code,
			positionId,
			workspaceMemberId
		);

		return ApiResponse.okWithNoContent("Position removed from workspace member.");
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{workspaceMemberId}/positions/{positionId}")
	public ApiResponse<AssignPositionResponse> assignPosition(
		@PathVariable String code,
		@PathVariable Long workspaceMemberId,
		@PathVariable Long positionId
	) {
		AssignPositionResponse response = workspaceMemberCommandService.assignPosition(
			code,
			positionId,
			workspaceMemberId
		);

		return ApiResponse.ok("Position assigned to workspace member.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{workspaceMemberId}/positions")
	public ApiResponse<Void> removePosition(
		@PathVariable String code,
		@PathVariable Long positionId,
		@PathVariable Long workspaceMemberId
	) {
		workspaceMemberCommandService.removePosition(
			code,
			positionId,
			workspaceMemberId
		);

		return ApiResponse.okWithNoContent("Position removed from workspace member.");
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@PatchMapping("/teams/{teamId}")
	public ApiResponse<AssignTeamResponse> assignMyTeam(
		@PathVariable String code,
		@PathVariable Long teamId,
		@CurrentWorkspaceMember Long workspaceMemberId
	) {
		AssignTeamResponse response = workspaceMemberCommandService.assignTeam(
			code,
			teamId,
			workspaceMemberId
		);

		return ApiResponse.ok("Team assigned to workspace member.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@DeleteMapping("/teams/{teamId}")
	public ApiResponse<Void> removeMyTeam(
		@PathVariable String code,
		@PathVariable Long teamId,
		@CurrentWorkspaceMember Long workspaceMemberId
	) {
		workspaceMemberCommandService.removeTeam(
			code,
			teamId,
			workspaceMemberId
		);

		return ApiResponse.okWithNoContent("Team removed from workspace member.");
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{workspaceMemberId}/teams/{teamId}")
	public ApiResponse<AssignTeamResponse> assignTeam(
		@PathVariable String code,
		@PathVariable Long workspaceMemberId,
		@PathVariable Long teamId
	) {
		AssignTeamResponse response = workspaceMemberCommandService.assignTeam(
			code,
			teamId,
			workspaceMemberId
		);

		return ApiResponse.ok("Team assigned to workspace member.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{workspaceMemberId}/teams")
	public ApiResponse<Void> removeTeam(
		@PathVariable String code,
		@PathVariable Long teamId,
		@PathVariable Long workspaceMemberId
	) {
		workspaceMemberCommandService.removeTeam(
			code,
			teamId,
			workspaceMemberId
		);

		return ApiResponse.okWithNoContent("Team removed from workspace member.");
	}
}
