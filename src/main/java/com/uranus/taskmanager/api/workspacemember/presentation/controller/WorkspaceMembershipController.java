package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authentication.resolver.LoginMember;
import com.uranus.taskmanager.api.security.authentication.resolver.ResolveLoginMember;
import com.uranus.taskmanager.api.security.authorization.interceptor.RoleRequired;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.RemoveMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.TransferOwnershipRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.RemoveMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateMemberRoleResponse;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceMemberCommandService;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceMemberInviteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
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
	@PostMapping("/{code}/members/invites")
	public ApiResponse<InviteMembersResponse> inviteMembers(
		@PathVariable String code,
		@RequestBody @Valid InviteMembersRequest request) {

		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers(code, request);
		return ApiResponse.ok("Members invited", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{code}/members/role")
	public ApiResponse<UpdateMemberRoleResponse> updateWorkspaceMemberRole(
		@PathVariable String code,
		@RequestBody @Valid UpdateMemberRoleRequest request,
		@ResolveLoginMember LoginMember loginMember
	) {

		UpdateMemberRoleResponse response = workspaceMemberCommandService.updateWorkspaceMemberRole(code,
			request,
			loginMember.getId());
		return ApiResponse.ok("Member's role for this workspace was updated", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.OWNER})
	@PatchMapping("/{code}/members/ownership")
	public ApiResponse<TransferOwnershipResponse> transferWorkspaceOwnership(
		@PathVariable String code,
		@RequestBody @Valid TransferOwnershipRequest request,
		@ResolveLoginMember LoginMember loginMember
	) {

		TransferOwnershipResponse response = workspaceMemberCommandService.transferWorkspaceOwnership(
			code,
			request,
			loginMember.getId()
		);
		return ApiResponse.ok("The ownership was successfully transfered", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@DeleteMapping("/{code}/members/kick")
	public ApiResponse<RemoveMemberResponse> kickWorkspaceMember(
		@PathVariable String code,
		@RequestBody @Valid RemoveMemberRequest request,
		@ResolveLoginMember LoginMember loginMember
	) {

		RemoveMemberResponse response = workspaceMemberCommandService.kickOutMember(
			code,
			request,
			loginMember.getId()
		);
		return ApiResponse.ok("Member was removed from this workspace", response);
	}
}
