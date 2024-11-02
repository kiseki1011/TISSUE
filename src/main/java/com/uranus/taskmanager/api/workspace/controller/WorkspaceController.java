package com.uranus.taskmanager.api.workspace.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.authentication.LoginMember;
import com.uranus.taskmanager.api.authentication.LoginRequired;
import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceContentUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceParticipateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspacePasswordUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspace.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceContentUpdateResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceCreateResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceParticipateResponse;
import com.uranus.taskmanager.api.workspace.service.WorkspaceCommandService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceCreateService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceQueryService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceService;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.authorization.RoleRequired;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

	/**
	 * Todo
	 *  - 워크스페이스 이름, 설명 수정 PATCH
	 *  - 워크스페이스 삭제 DELETE
	 *  - 워크스페이스 비밀번호 설정(만약 없으면 설정, 있으면 수정)
	 *  - 워크스페이스 상세 정보 조회(워크스페이스 코드를 통해, 해당 워크스페이스의 멤버여야 함) GET
	 *  - 내가 참여 중인 모든 워크스페이스 조회하기
	 */

	private final WorkspaceService workspaceService;
	private final WorkspaceCreateService workspaceCreateService;
	private final WorkspaceCommandService workspaceCommandService;
	private final WorkspaceQueryService workspaceQueryService;

	@LoginRequired
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<WorkspaceCreateResponse> createWorkspace(
		@LoginMember LoginMemberDto loginMember,
		@RequestBody @Valid WorkspaceCreateRequest request) {

		WorkspaceCreateResponse response = workspaceCreateService.createWorkspace(request, loginMember);
		return ApiResponse.created("Workspace Created", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@DeleteMapping("/{code}")
	public ApiResponse<String> deleteWorkspace(@PathVariable String code,
		@RequestBody WorkspaceDeleteRequest request) {

		workspaceCommandService.deleteWorkspace(request, code);
		return ApiResponse.ok("Workspace Deleted", code);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@PatchMapping("/{code}")
	public ApiResponse<WorkspaceContentUpdateResponse> updateWorkspaceContent(@PathVariable String code,
		@RequestBody @Valid WorkspaceContentUpdateRequest request) {

		WorkspaceContentUpdateResponse response = workspaceCommandService.updateWorkspaceContent(request, code);
		return ApiResponse.ok("Workspace Title and Description Updated", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.ADMIN)
	@PutMapping("/{code}/password")
	public ApiResponse<String> updateWorkspacePassword(@PathVariable String code,
		@RequestBody @Valid WorkspacePasswordUpdateRequest request) {

		workspaceCommandService.updateWorkspacePassword(request, code);
		return ApiResponse.okWithNoContent("Workspace Password Updated");
	}

	@LoginRequired
	@GetMapping("/{code}")
	public ApiResponse<WorkspaceDetail> getWorkspaceDetail(@PathVariable String code,
		@LoginMember LoginMemberDto loginMember) {

		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(code, loginMember);
		return ApiResponse.ok("Workspace Found", response);
	}

	@LoginRequired
	@GetMapping
	public ApiResponse<MyWorkspacesResponse> getMyWorkspaces(@LoginMember LoginMemberDto loginMember,
		Pageable pageable) {

		MyWorkspacesResponse response = workspaceQueryService.getMyWorkspaces(loginMember, pageable);
		return ApiResponse.ok("Currently joined Workspaces Found", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.ADMIN})
	@PostMapping("/{code}/invite")
	public ApiResponse<InviteMemberResponse> inviteMember(
		@PathVariable String code,
		@LoginMember LoginMemberDto loginMember,
		@RequestBody @Valid InviteMemberRequest request) {

		InviteMemberResponse response = workspaceService.inviteMember(code, request, loginMember);
		return ApiResponse.ok("Member Invited", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.ADMIN})
	@PostMapping("/{code}/invites")
	public ApiResponse<InviteMembersResponse> inviteMembers(
		@PathVariable String code,
		@LoginMember LoginMemberDto loginMember,
		@RequestBody @Valid InviteMembersRequest request) {

		InviteMembersResponse response = workspaceService.inviteMembers(code, request, loginMember);
		return ApiResponse.ok("Members Invited", response);
	}

	/*
	 * Todo
	 *  - participate -> join으로 명칭 변경
	 *  - MemberAlreadyParticipationException -> AlreadyJoinedWorkspaceException 명칭 변경
	 */
	@LoginRequired
	@PostMapping("/{code}")
	public ApiResponse<WorkspaceParticipateResponse> participateWorkspace(
		@PathVariable String code,
		@LoginMember LoginMemberDto loginMember,
		@RequestBody @Valid WorkspaceParticipateRequest request
	) {

		WorkspaceParticipateResponse response = workspaceService.participateWorkspace(code, request, loginMember);
		return ApiResponse.ok("Joined Workspace", response);
	}

}
