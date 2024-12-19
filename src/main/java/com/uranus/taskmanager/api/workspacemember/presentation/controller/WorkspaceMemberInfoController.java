package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.dto.ApiResponse;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authentication.resolver.ResolveLoginMember;
import com.uranus.taskmanager.api.security.authorization.interceptor.RoleRequired;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateNicknameRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.AssignPositionResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateNicknameResponse;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceMemberCommandService;

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
	 *  - udpateNickname: 내 별칭 수정 (VIEWER)
	 *  - assignPosition: 내 포지션 할당 (VIEWER)
	 *  - removePosition: 내 포지션 해제 (VIEWER)
	 *  - assignMemberPosition: 특정 멤버의 포지션 할당 (MANAGER)
	 *  - removeMemberPosition: 특정 멤버의 포지션 해제 (MANAGER)
	 *  - <br>
	 *  - 구현 예정
	 *  - setNicknameSchema: 워크스페이스의 별칭 스키마 정하기 (OWNER)
	 *    - 예시: [멤버의 소속 부서/팀][멤버의 포지션]멤버의 이름 -> 이렇게 정하는 것이 가능, 중복되는 경우 숫자 붙이기
	 *    - 예시: [SearchTeam][BACKEND-DEV]HongGilDong
	 *  - assignTeam: 내 팀(부서) 소속 할당 (VIEWER)
	 *  - removeTeam: 내 팀(부서) 소속 해제 (VIEWER)
	 *  - assignMemberTeam: 특정 멤버의 팀(부서) 소속 할당 (MANAGER)
	 *  - removeMemberTeam: 특정 멤버의 팀(부서) 소속 해제 (MANAGER)
	 *  - <br>
	 *  - 필요한 도메인
	 *  - Team(Department, 소속)
	 *  - Level(경력)
	 */

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@PatchMapping("/nickname")
	public ApiResponse<UpdateNicknameResponse> updateNickname(
		@PathVariable String code,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid UpdateNicknameRequest request
	) {
		UpdateNicknameResponse response = workspaceMemberCommandService.updateNickname(
			code,
			loginMemberId,
			request
		);
		return ApiResponse.ok("Nickname updated.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@PatchMapping("/positions/{positionId}")
	public ApiResponse<AssignPositionResponse> assignPosition(
		@PathVariable String code,
		@PathVariable Long positionId,
		@ResolveLoginMember Long loginMemberId
	) {
		AssignPositionResponse response = workspaceMemberCommandService.assignPosition(
			code,
			positionId,
			loginMemberId
		);
		return ApiResponse.ok("Position assigned to member.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@PatchMapping("/positions")
	public ApiResponse<Void> removePosition(
		@PathVariable String code,
		@ResolveLoginMember Long loginMemberId
	) {
		workspaceMemberCommandService.removePosition(
			code,
			loginMemberId
		);
		return ApiResponse.okWithNoContent("Position removed from member.");
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{memberId}/positions/{positionId}")
	public ApiResponse<AssignPositionResponse> assignMemberPosition(
		@PathVariable String code,
		@PathVariable Long memberId,
		@PathVariable Long positionId
	) {
		AssignPositionResponse response = workspaceMemberCommandService.assignPosition(
			code,
			positionId,
			memberId
		);
		return ApiResponse.ok("Position assigned to member.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{memberId}/positions")
	public ApiResponse<Void> removeMemberPosition(
		@PathVariable String code,
		@PathVariable Long memberId
	) {
		workspaceMemberCommandService.removePosition(
			code,
			memberId
		);
		return ApiResponse.okWithNoContent("Position removed from member.");
	}
}
