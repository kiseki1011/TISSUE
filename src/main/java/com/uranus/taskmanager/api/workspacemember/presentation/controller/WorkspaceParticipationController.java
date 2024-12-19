package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.dto.ApiResponse;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authentication.resolver.ResolveLoginMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.JoinWorkspaceResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceParticipationCommandService;
import com.uranus.taskmanager.api.workspacemember.service.query.WorkspaceParticipationQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceParticipationController {

	private final WorkspaceParticipationQueryService workspaceParticipationQueryService;
	private final WorkspaceParticipationCommandService workspaceParticipationCommandService;

	/**
	 * Todo
	 *  - leaveWorkspace: 특정 참여한 워크스페이스 떠나기
	 *    - 해당 워크스페이스의 OWNER이면 안됨
	 *  - getMyWorkspaceRole: 특정 참여한 워크스페이스에서 내가 가지고 있는 권한 조회하기
	 */

	@LoginRequired
	@PostMapping("/{code}")
	public ApiResponse<JoinWorkspaceResponse> joinWorkspace(
		@PathVariable String code,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid JoinWorkspaceRequest request
	) {

		JoinWorkspaceResponse response = workspaceParticipationCommandService.joinWorkspace(
			code,
			request,
			loginMemberId
		);
		return ApiResponse.ok("Joined workspace", response);
	}

	@LoginRequired
	@GetMapping
	public ApiResponse<MyWorkspacesResponse> getMyWorkspaces(
		@ResolveLoginMember Long loginMemberId,
		Pageable pageable
	) {

		MyWorkspacesResponse response = workspaceParticipationQueryService.getMyWorkspaces(
			loginMemberId,
			pageable
		);
		return ApiResponse.ok("Currently joined workspaces found.", response);
	}
}
