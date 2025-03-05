package com.tissue.api.issue.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.AssignParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.UpdateIssueStatusResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.issue.presentation.dto.response.delete.DeleteIssueResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateIssueResponse;
import com.tissue.api.issue.service.command.IssueCommandService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/issues")
public class IssueController {

	private final IssueCommandService issueCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<CreateIssueResponse> createIssue(
		@PathVariable String code,
		@RequestBody @Valid CreateIssueRequest request
	) {
		CreateIssueResponse response = issueCommandService.createIssue(code, request);

		return ApiResponse.ok(response.getType() + " issue created.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/status")
	public ApiResponse<UpdateIssueStatusResponse> updateIssueStatus(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long currentWorkspaceMemberId,
		@RequestBody @Valid UpdateIssueStatusRequest request
	) {
		UpdateIssueStatusResponse response = issueCommandService.updateIssueStatus(
			code,
			issueKey,
			currentWorkspaceMemberId,
			request
		);

		return ApiResponse.ok("Issue status updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}")
	public ApiResponse<UpdateIssueResponse> updateIssueDetail(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long currentWorkspaceMemberId,
		@RequestBody @Valid UpdateIssueRequest request
	) {
		UpdateIssueResponse response = issueCommandService.updateIssue(
			code,
			issueKey,
			currentWorkspaceMemberId,
			request
		);

		return ApiResponse.ok("Issue details updated.", response);
	}

	// @LoginRequired
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{issueKey}/storypoint")
	// public ApiResponse<UpdateIssueResponse> updateIssueStoryPoint(
	// 	@PathVariable String code,
	// 	@PathVariable String issueKey,
	// 	@CurrentWorkspaceMember Long currentWorkspaceMemberId,
	// 	@RequestBody @Valid UpdateStoryPointRequest request
	// ) {
	// 	UpdateIssueResponse response = issueCommandService.updateStoryPoint(
	// 		code,
	// 		issueKey,
	// 		currentWorkspaceMemberId,
	// 		request
	// 	);
	//
	// 	return ApiResponse.ok("Issue details updated.", response);
	// }

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/parent")
	public ApiResponse<AssignParentIssueResponse> assignParentIssue(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long currentWorkspaceMemberId,
		@RequestBody @Valid AssignParentIssueRequest request
	) {
		AssignParentIssueResponse response = issueCommandService.assignParentIssue(
			code,
			issueKey,
			currentWorkspaceMemberId,
			request
		);

		return ApiResponse.ok("Parent issue assigned.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/parent")
	public ApiResponse<RemoveParentIssueResponse> removeParentIssue(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long currentWorkspaceMemberId
	) {
		RemoveParentIssueResponse response = issueCommandService.removeParentIssue(
			code,
			issueKey,
			currentWorkspaceMemberId
		);

		return ApiResponse.ok("Parent issue relationship removed.", response);
	}

	/**
	 * Todo
	 *  - hard delete을 사용하지 말고, 이슈 상태를 CLOSED로 바꾸는 soft delete 고려
	 */
	@Deprecated
	@LoginRequired
	@RoleRequired(role = WorkspaceRole.ADMIN)
	@DeleteMapping("/{issueKey}")
	public ApiResponse<DeleteIssueResponse> deleteIssue(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long currentWorkspaceMemberId
	) {
		DeleteIssueResponse response = issueCommandService.deleteIssue(
			code,
			issueKey,
			currentWorkspaceMemberId
		);

		return ApiResponse.ok("Issue deleted.", response);
	}
}
