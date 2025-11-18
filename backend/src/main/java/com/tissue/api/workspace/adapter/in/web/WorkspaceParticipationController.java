package com.tissue.api.workspace.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspace.domain.service.WorkspaceAuthenticationService;
import com.tissue.api.workspace.application.service.WorkspaceParticipationService;
import com.tissue.api.workspace.application.service.WorkspaceParticipationQueryService;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

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

	//
	// @PostMapping("/{workspaceKey}/members")
	// public ApiResponse<WorkspaceMemberResponse> joinWorkspace(
	// 	@PathVariable String workspaceCode,
	// 	@CurrentMember MemberUserDetails userDetails,
	// 	@RequestBody @Valid JoinWorkspaceRequest request
	// ) {
	// 	workspaceAuthenticationService.authenticate(request.password(), workspaceCode);
	// 	WorkspaceMemberResponse response = workspaceParticipationService.joinWorkspace(
	// 		workspaceCode,
	// 		userDetails.getMemberId()
	// 	);
	//
	// 	return ApiResponse.ok("Joined workspace", response);
	// }

	@RoleRequired(role = WorkspaceRole.MEMBER)
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

	// TODO: MemberQueryController로 이동하는게 좋을 듯
	// @GetMapping
	// public ApiResponse<GetWorkspacesResponse> getWorkspaces(
	// 	@CurrentMember MemberUserDetails userDetails,
	// 	Pageable pageable
	// ) {
	// 	GetWorkspacesResponse response = workspaceParticipationQueryService.getWorkspaces(
	// 		userDetails.getMemberId(),
	// 		pageable
	// 	);
	//
	// 	return ApiResponse.ok("Currently joined workspaces found.", response);
	// }
}
