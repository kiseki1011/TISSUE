package com.tissue.api.workspace.adapter.in.web;

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
import com.tissue.api.workspace.application.dto.request.AssignPositionCommand;
import com.tissue.api.workspace.application.dto.request.AssignTeamCommand;
import com.tissue.api.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.api.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.api.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.api.workspace.application.service.WorkspaceMemberService;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;
import com.tissue.api.workspace.adapter.in.web.dto.request.AssignPositionRequest;
import com.tissue.api.workspace.adapter.in.web.dto.request.AssignTeamRequest;
import com.tissue.api.workspace.adapter.in.web.dto.request.RemovePositionRequest;
import com.tissue.api.workspace.adapter.in.web.dto.request.RemoveTeamRequest;
import com.tissue.api.workspace.adapter.in.web.dto.request.UpdateDisplayNameRequest;
import com.tissue.api.workspace.adapter.in.web.dto.response.WorkspaceMemberResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
public class WorkspaceMemberController {

	private final WorkspaceMemberService workspaceMemberService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
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

	/**
	 * TODO: assign/remove Position/Team에 대해서 본인이 하는 것만 허용. 그 외에는 ADMIN 이상만 변경을 허용.
	 *  - 이렇게 구현하기 위해서는 하나의 API를 두고 안에서 권한 검증을 할까? 아니면 엔드 포인트를 따로 둘까?
	 *  - 본인 검사는 어차피 userDetails.getMemberId()로 꺼내기 때문에 할 필요 없을 듯.
	 *  - ADMIN 이상의 WorkspaceMember가 남의 position을 변경하는 경우에는 스프링 시큐리티의 @Authorized를 WorkspaceRole과 활용?
	 */
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
