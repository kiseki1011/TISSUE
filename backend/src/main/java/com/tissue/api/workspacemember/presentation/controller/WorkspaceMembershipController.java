package com.tissue.api.workspacemember.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.security.authorization.interceptor.SelfOrRoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateRoleRequest;
import com.tissue.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.tissue.api.workspacemember.presentation.dto.response.RemoveWorkspaceMemberResponse;
import com.tissue.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateRoleResponse;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberCommandService;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberInviteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/members")
public class WorkspaceMembershipController {

	private final WorkspaceMemberCommandService workspaceMemberCommandService;
	private final WorkspaceMemberInviteService workspaceMemberInviteService;

	/**
	 * Todo
	 *  - getWorkspaceMembers: 특정 워크스페이스에서 존재하는 모든 멤버 목록 조회
	 *    - 페이징 적용
	 *    - 조건에 따른 검색 적용 필요(QueryDSL 사용할까?)
	 */
	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/invite")
	public ApiResponse<InviteMembersResponse> inviteMembers(
		@PathVariable String workspaceCode,
		@RequestBody @Valid InviteMembersRequest request
	) {
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers(
			workspaceCode,
			request
		);

		return ApiResponse.ok("Members invited", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{memberId}/role")
	public ApiResponse<UpdateRoleResponse> updateWorkspaceMemberRole(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid UpdateRoleRequest request
	) {
		UpdateRoleResponse response = workspaceMemberCommandService.updateRole(
			workspaceCode,
			memberId,
			loginMemberId,
			request
		);

		return ApiResponse.ok("Member's role for this workspace was updated", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.OWNER)
	@PatchMapping("/{memberId}/ownership")
	public ApiResponse<TransferOwnershipResponse> transferWorkspaceOwnership(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId
	) {
		TransferOwnershipResponse response = workspaceMemberCommandService.transferWorkspaceOwnership(
			workspaceCode,
			memberId,
			loginMemberId
		);

		return ApiResponse.ok("The ownership was successfully transfered", response);
	}

	@LoginRequired
	@SelfOrRoleRequired(role = WorkspaceRole.ADMIN, memberIdParam = "memberId")
	@DeleteMapping("/{memberId}")
	public ApiResponse<RemoveWorkspaceMemberResponse> removeWorkspaceMember(
		@PathVariable String workspaceCode,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId
	) {
		RemoveWorkspaceMemberResponse response = workspaceMemberCommandService.removeWorkspaceMember(
			workspaceCode,
			memberId,
			loginMemberId
		);

		return ApiResponse.ok("Member was removed from this workspace", response);
	}
}
