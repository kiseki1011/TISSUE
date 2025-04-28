package com.tissue.api.workspacemember.presentation.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.workspace.validator.WorkspaceValidator;
import com.tissue.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.tissue.api.workspacemember.presentation.dto.response.GetWorkspacesResponse;
import com.tissue.api.workspacemember.presentation.dto.response.JoinWorkspaceResponse;
import com.tissue.api.workspacemember.service.command.WorkspaceParticipationCommandService;
import com.tissue.api.workspacemember.service.query.WorkspaceParticipationQueryService;

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
	private final WorkspaceValidator workspaceValidator;

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
		workspaceValidator.validateWorkspacePassword(request.password(), code);
		JoinWorkspaceResponse response = workspaceParticipationCommandService.joinWorkspace(code, loginMemberId);
		return ApiResponse.ok("Joined workspace", response);
	}

	@LoginRequired
	@GetMapping
	public ApiResponse<GetWorkspacesResponse> getWorkspaces(
		@ResolveLoginMember Long loginMemberId,
		Pageable pageable
	) {
		GetWorkspacesResponse response = workspaceParticipationQueryService.getWorkspaces(
			loginMemberId,
			pageable
		);
		return ApiResponse.ok("Currently joined workspaces found.", response);
	}
}
