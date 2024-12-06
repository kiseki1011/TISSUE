package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.dto.ApiResponse;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authentication.resolver.ResolveLoginMember;
import com.uranus.taskmanager.api.security.authorization.interceptor.RoleRequired;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateMemberNicknameRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.RemoveMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateMemberNicknameResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateMemberRoleResponse;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceMemberCommandService;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceMemberInviteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/members")
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
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PostMapping("/invite")
	public ApiResponse<InviteMembersResponse> inviteMembers(
		@PathVariable String code,
		@RequestBody @Valid InviteMembersRequest request
	) {
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers(
			code,
			request
		);
		return ApiResponse.ok("Members invited", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@PatchMapping("/nickname")
	public ApiResponse<UpdateMemberNicknameResponse> updateMemberNickname(
		@PathVariable String code,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid UpdateMemberNicknameRequest request
	) {
		UpdateMemberNicknameResponse response = workspaceMemberCommandService.updateWorkspaceMemberNickname(
			code,
			loginMemberId,
			request
		);
		return ApiResponse.ok("Nickname updated.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{memberId}/role")
	public ApiResponse<UpdateMemberRoleResponse> updateWorkspaceMemberRole(
		@PathVariable String code,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid UpdateMemberRoleRequest request
	) {
		UpdateMemberRoleResponse response = workspaceMemberCommandService.updateWorkspaceMemberRole(
			code,
			memberId,
			loginMemberId,
			request
		);
		return ApiResponse.ok("Member's role for this workspace was updated", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.OWNER})
	@PatchMapping("/{memberId}/ownership")
	public ApiResponse<TransferOwnershipResponse> transferWorkspaceOwnership(
		@PathVariable String code,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId
	) {
		TransferOwnershipResponse response = workspaceMemberCommandService.transferWorkspaceOwnership(
			code,
			memberId,
			loginMemberId
		);
		return ApiResponse.ok("The ownership was successfully transfered", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@DeleteMapping("/{memberId}")
	public ApiResponse<RemoveMemberResponse> removeMember(
		@PathVariable String code,
		@PathVariable Long memberId,
		@ResolveLoginMember Long loginMemberId
	) {
		RemoveMemberResponse response = workspaceMemberCommandService.removeMember(
			code,
			memberId,
			loginMemberId
		);
		return ApiResponse.ok("Member was removed from this workspace", response);
	}
}
