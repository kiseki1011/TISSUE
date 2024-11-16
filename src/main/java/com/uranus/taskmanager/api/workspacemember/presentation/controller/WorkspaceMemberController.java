package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.global.interceptor.LoginRequired;
import com.uranus.taskmanager.api.global.interceptor.RoleRequired;
import com.uranus.taskmanager.api.global.resolver.ResolveLoginMember;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.LoginMember;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.KickWorkspaceMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.WorkspaceJoinRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.KickWorkspaceMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateWorkspaceMemberRoleResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.WorkspaceJoinResponse;
import com.uranus.taskmanager.api.workspacemember.service.WorkspaceMemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceMemberController {

	private final WorkspaceMemberService workspaceMemberService;

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PostMapping("/{code}/invite")
	public ApiResponse<InviteMemberResponse> inviteMember(
		@PathVariable String code,
		@RequestBody @Valid InviteMemberRequest request) {

		InviteMemberResponse response = workspaceMemberService.inviteMember(code, request);
		return ApiResponse.ok("Member invited", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PostMapping("/{code}/invites")
	public ApiResponse<InviteMembersResponse> inviteMembers(
		@PathVariable String code,
		@RequestBody @Valid InviteMembersRequest request) {

		InviteMembersResponse response = workspaceMemberService.inviteMembers(code, request);
		return ApiResponse.ok("Members invited", response);
	}

	@LoginRequired
	@PostMapping("/{code}")
	public ApiResponse<WorkspaceJoinResponse> joinWorkspace(
		@PathVariable String code,
		@ResolveLoginMember LoginMember loginMember,
		@RequestBody @Valid WorkspaceJoinRequest request
	) {

		WorkspaceJoinResponse response = workspaceMemberService.joinWorkspace(code, request, loginMember.getId());
		return ApiResponse.ok("Joined workspace", response);
	}

	/**
	 * Todo
	 *  - 자기 자신 강퇴 불가능하게 로직 수정
	 *  - 자기보다 낮은 권한만 강퇴할 수 있도록 로직 추가
	 */
	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@DeleteMapping("/{code}/kick")
	public ApiResponse<KickWorkspaceMemberResponse> kickWorkspaceMember(
		@PathVariable String code,
		@RequestBody @Valid KickWorkspaceMemberRequest request
	) {

		KickWorkspaceMemberResponse response = workspaceMemberService.kickWorkspaceMember(code, request);
		return ApiResponse.ok("Member was kicked from this workspace", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@PatchMapping("/{code}/members/role")
	public ApiResponse<UpdateWorkspaceMemberRoleResponse> updateWorkspaceMemberRole(
		@PathVariable String code,
		@RequestBody @Valid UpdateWorkspaceMemberRoleRequest request,
		@ResolveLoginMember LoginMember loginMember
	) {

		UpdateWorkspaceMemberRoleResponse response = workspaceMemberService.updateWorkspaceMemberRole(code, request,
			loginMember.getId());
		return ApiResponse.ok("Member's role for this workspace was updated", response);
	}
}
