package com.uranus.taskmanager.api.workspace.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.authentication.LoginRequired;
import com.uranus.taskmanager.api.authentication.ResolveLoginMember;
import com.uranus.taskmanager.api.authentication.dto.LoginMember;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspace.dto.request.KickWorkspaceMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceJoinRequest;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspace.dto.response.KickWorkspaceMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceJoinResponse;
import com.uranus.taskmanager.api.workspace.service.WorkspaceAccessService;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.authorization.RoleRequired;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceAccessController {

	private final WorkspaceAccessService workspaceAccessService;

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.ADMIN})
	@PostMapping("/{code}/invite")
	public ApiResponse<InviteMemberResponse> inviteMember(
		@PathVariable String code,
		@RequestBody @Valid InviteMemberRequest request) {

		InviteMemberResponse response = workspaceAccessService.inviteMember(code, request);
		return ApiResponse.ok("Member Invited", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.ADMIN})
	@PostMapping("/{code}/invites")
	public ApiResponse<InviteMembersResponse> inviteMembers(
		@PathVariable String code,
		@RequestBody @Valid InviteMembersRequest request) {

		InviteMembersResponse response = workspaceAccessService.inviteMembers(code, request);
		return ApiResponse.ok("Members Invited", response);
	}

	@LoginRequired
	@PostMapping("/{code}")
	public ApiResponse<WorkspaceJoinResponse> joinWorkspace(
		@PathVariable String code,
		@ResolveLoginMember LoginMember loginMember,
		@RequestBody @Valid WorkspaceJoinRequest request
	) {

		WorkspaceJoinResponse response = workspaceAccessService.joinWorkspace(code, request, loginMember.getId());
		return ApiResponse.ok("Joined Workspace", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.ADMIN})
	@DeleteMapping("/{code}/kick")
	public ApiResponse<KickWorkspaceMemberResponse> kickWorkspaceMember(
		@PathVariable String code,
		@RequestBody @Valid KickWorkspaceMemberRequest request
	) {

		KickWorkspaceMemberResponse response = workspaceAccessService.kickWorkspaceMember(code, request);
		return ApiResponse.ok("Member was kicked from this Workspace", response);
	}
}
