package com.tissue.api.workspacemember.presentation.controller.command;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspace.domain.service.WorkspaceAuthenticationService;
import com.tissue.api.workspacemember.application.service.command.WorkspaceParticipationService;
import com.tissue.api.workspacemember.application.service.query.WorkspaceParticipationQueryService;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.tissue.api.workspacemember.presentation.dto.response.GetWorkspacesResponse;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceParticipationController {

	private final WorkspaceParticipationQueryService workspaceParticipationQueryService;
	private final WorkspaceParticipationService workspaceParticipationService;
	private final WorkspaceAuthenticationService workspaceAuthenticationService;

	/**
	 * Todo
	 *  - leaveWorkspace: 특정 참여한 워크스페이스 떠나기
	 *    - 해당 워크스페이스의 OWNER이면 안됨
	 *    - removeWorkspaceMember에서 자기 자신도 가능하도록 설계했는데, 이걸 변경
	 *    - removeWorkspaceMember는 ADMIN 이상, OWNER x, 자기자신 x 인 경우만 가능하도록
	 *  - getMyWorkspaceRole: 특정 참여한 워크스페이스에서 내가 가지고 있는 권한 조회하기
	 */

	@PostMapping("/{workspaceKey}/members")
	public ApiResponse<WorkspaceMemberResponse> joinWorkspace(
		@PathVariable String workspaceCode,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid JoinWorkspaceRequest request
	) {
		workspaceAuthenticationService.authenticate(request.password(), workspaceCode);
		WorkspaceMemberResponse response = workspaceParticipationService.joinWorkspace(
			workspaceCode,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Joined workspace", response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{workspaceKey}/members")
	public ApiResponse<Void> leaveWorkspace(
		@PathVariable String workspaceCode,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceParticipationService.leaveWorkspace(
			workspaceCode,
			userDetails.getMemberId()
		);

		return ApiResponse.okWithNoContent("Leaved workspace");
	}

	@GetMapping
	public ApiResponse<GetWorkspacesResponse> getWorkspaces(
		@CurrentMember MemberUserDetails userDetails,
		Pageable pageable
	) {
		GetWorkspacesResponse response = workspaceParticipationQueryService.getWorkspaces(
			userDetails.getMemberId(),
			pageable
		);

		return ApiResponse.ok("Currently joined workspaces found.", response);
	}
}
