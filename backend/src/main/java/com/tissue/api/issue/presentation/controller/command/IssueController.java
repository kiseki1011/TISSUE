package com.tissue.api.issue.presentation.controller.command;

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
import com.tissue.api.issue.application.service.command.IssueCommandService;
import com.tissue.api.issue.presentation.controller.dto.request.AddParentIssueRequest;
import com.tissue.api.issue.presentation.controller.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.controller.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.controller.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/issues")
public class IssueController {

	private final IssueCommandService issueCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<IssueResponse> createIssue(
		@PathVariable String workspaceCode,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid CreateIssueRequest request
	) {
		IssueResponse response = issueCommandService.createIssue(workspaceCode, loginMemberId, request);

		return ApiResponse.created("Issue created.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/status")
	public ApiResponse<IssueResponse> updateIssueStatus(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid UpdateIssueStatusRequest request
	) {
		IssueResponse response = issueCommandService.updateIssueStatus(
			workspaceCode,
			issueKey,
			loginMemberId,
			request
		);

		return ApiResponse.ok("Issue status updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}")
	public ApiResponse<IssueResponse> updateIssueDetail(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid UpdateIssueRequest request
	) {
		IssueResponse response = issueCommandService.updateIssue(
			workspaceCode,
			issueKey,
			loginMemberId,
			request
		);

		return ApiResponse.ok("Issue details updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> assignParentIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid AddParentIssueRequest request
	) {
		IssueResponse response = issueCommandService.assignParentIssue(
			workspaceCode,
			issueKey,
			loginMemberId,
			request
		);

		return ApiResponse.ok("Parent issue assigned.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> removeParentIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@ResolveLoginMember Long loginMemberId
	) {
		IssueResponse response = issueCommandService.removeParentIssue(
			workspaceCode,
			issueKey,
			loginMemberId
		);

		return ApiResponse.ok("Parent issue relationship removed.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PostMapping("{issueKey}/watch")
	public ApiResponse<IssueResponse> watchIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@ResolveLoginMember Long loginMemberId
	) {
		IssueResponse response = issueCommandService.watchIssue(
			workspaceCode,
			issueKey,
			loginMemberId
		);

		return ApiResponse.ok("Watching issue.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@DeleteMapping("{issueKey}/watch")
	public ApiResponse<IssueResponse> unwatchIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@ResolveLoginMember Long loginMemberId
	) {
		IssueResponse response = issueCommandService.unwatchIssue(
			workspaceCode,
			issueKey,
			loginMemberId
		);

		return ApiResponse.ok("Unwatched issue.", response);
	}
}
