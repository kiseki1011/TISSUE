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
import com.tissue.api.workspacemember.application.dto.AssignPositionCommand;
import com.tissue.api.workspacemember.application.dto.AssignTeamCommand;
import com.tissue.api.workspacemember.application.dto.RemovePositionCommand;
import com.tissue.api.workspacemember.application.dto.RemoveTeamCommand;
import com.tissue.api.workspacemember.application.dto.UpdateDisplayNameCommand;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberService;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.AssignPositionRequest;
import com.tissue.api.workspacemember.presentation.dto.request.AssignTeamRequest;
import com.tissue.api.workspacemember.presentation.dto.request.RemovePositionRequest;
import com.tissue.api.workspacemember.presentation.dto.request.RemoveTeamRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateDisplayNameRequest;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
public class WorkspaceMemberController {

	private final WorkspaceMemberService workspaceMemberService;

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PatchMapping("/displayname")
	public ApiResponse<WorkspaceMemberResponse> updateDisplayName(
		@PathVariable String workspaceKey,
		@RequestBody @Valid UpdateDisplayNameRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberService.updateDisplayName(
			new UpdateDisplayNameCommand(workspaceKey, userDetails.getMemberId(), request.displayName())
		);

		return ApiResponse.ok("Display name updated.", response);
	}

	// TODO: 기존에는 SelfOrRoleRequired + /{memberId}/positions/{positionId} 경로를 사용.
	//  MANAGER 이상의 권한이거나 자기자신이면 해당 API를 사용할 수 있도록 인터셉터에서 검증하는 방식.
	//  그러나 내가 내 Position이나 Team을 설정하는 API랑, 남이 지정하는 API랑 분리할 생각
	//  내가 설정하는 API의 경로는 workspace/{workspaceKey}/members/positions - positionId는 request body 안에
	//  남이 설정하는 API의 경로는 workspace/{workspaceKey}/members/{memberId}/positions
	// @SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/positions")
	public ApiResponse<WorkspaceMemberResponse> assignPosition(
		@PathVariable String workspaceKey,
		@RequestBody @Valid AssignPositionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberService.assignPosition(
			new AssignPositionCommand(workspaceKey, userDetails.getMemberId(), request.positionId())
		);

		return ApiResponse.ok("Position assigned to workspace member.", response);
	}

	// @SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/positions")
	public ApiResponse<WorkspaceMemberResponse> removePosition(
		@PathVariable String workspaceKey,
		@RequestBody @Valid RemovePositionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberService.removePosition(
			new RemovePositionCommand(workspaceKey, userDetails.getMemberId(), request.positionId())
		);

		return ApiResponse.ok("Position removed from workspace member.", response);
	}

	// @SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/teams")
	public ApiResponse<WorkspaceMemberResponse> setTeam(
		@PathVariable String workspaceKey,
		@RequestBody @Valid AssignTeamRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberService.assignTeam(
			new AssignTeamCommand(workspaceKey, userDetails.getMemberId(), request.teamId())
		);

		return ApiResponse.ok("Team assigned to workspace member.", response);
	}

	// @SelfOrRoleRequired(role = WorkspaceRole.MANAGER, memberIdParam = "memberId")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/teams")
	public ApiResponse<WorkspaceMemberResponse> removeTeam(
		@PathVariable String workspaceKey,
		@RequestBody @Valid RemoveTeamRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberService.removeTeam(
			new RemoveTeamCommand(workspaceKey, userDetails.getMemberId(), request.teamId())
		);

		return ApiResponse.ok("Team removed from workspace member.", response);
	}
}
