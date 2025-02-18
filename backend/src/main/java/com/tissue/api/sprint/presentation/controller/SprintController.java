package com.tissue.api.sprint.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.sprint.presentation.dto.request.AddSprintIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.CreateSprintRequest;
import com.tissue.api.sprint.presentation.dto.response.AddSprintIssuesResponse;
import com.tissue.api.sprint.presentation.dto.response.CreateSprintResponse;
import com.tissue.api.sprint.presentation.dto.response.SprintDetailResponse;
import com.tissue.api.sprint.service.command.SprintCommandService;
import com.tissue.api.sprint.service.query.SprintQueryService;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceCode}/sprints")
public class SprintController {

	private final SprintCommandService sprintCommandService;
	private final SprintQueryService sprintQueryService;

	/*
	 * Todo
	 *  - 스프린트 생성
	 *  - 스프린트 수정(이름, 목표, 종료일)
	 *  - 스프린트 상태 변경
	 *   - 기본 상태: PLANNING
	 *   - ACTIVE, COMPLETED, CANCELLED(hard delete 대신)
	 *  - 스프린트 이슈 등록(다중 등록)
	 *  - 스프린트 이슈 해제
	 */

	@LoginRequired
	@ResponseStatus(HttpStatus.CREATED)
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<CreateSprintResponse> createSprint(
		@PathVariable String workspaceCode,
		@RequestBody @Valid CreateSprintRequest request
	) {
		CreateSprintResponse response = sprintCommandService.createSprint(workspaceCode, request);
		return ApiResponse.ok("Sprint created.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@GetMapping("/{sprintKey}")
	public ApiResponse<SprintDetailResponse> getSprintDetail(
		@PathVariable String workspaceCode,
		@PathVariable String sprintKey
	) {
		SprintDetailResponse response = sprintQueryService.getSprintDetail(workspaceCode, sprintKey);
		return ApiResponse.ok("Found sprint.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{sprintKey}/issues")
	public ApiResponse<AddSprintIssuesResponse> addIssues(
		@PathVariable String workspaceCode,
		@PathVariable String sprintKey,
		@RequestBody @Valid AddSprintIssuesRequest request
	) {
		AddSprintIssuesResponse response = sprintCommandService.addIssues(
			workspaceCode,
			sprintKey,
			request
		);
		return ApiResponse.ok("Issues added to sprint.", response);
	}
	//
	// @LoginRequired
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{sprintKey}/status")
	// public ApiResponse<UpdateSprintStatusResponse> updateSprintStatus(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String sprintKey,
	// 	@CurrentWorkspaceMember Long currentWorkspaceMemberId,
	// 	@RequestBody @Valid UpdateSprintStatusRequest request
	// ) {
	// 	UpdateSprintStatusResponse response = sprintCommandService.updateSprintStatus(
	// 		code,
	// 		id,
	// 		currentWorkspaceMemberId,
	// 		request
	// 	);
	// 	return ApiResponse.ok("Sprint status updated successfully.", response);
	// }
}
