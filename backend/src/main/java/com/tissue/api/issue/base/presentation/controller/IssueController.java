package com.tissue.api.issue.base.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.base.application.service.IssueService;
import com.tissue.api.issue.base.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.base.presentation.dto.request.CreateIssueRequest;
import com.tissue.api.issue.base.presentation.dto.request.UpdateIssueRequest;
import com.tissue.api.issue.base.presentation.dto.response.IssueResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/issues")
public class IssueController {

	private final IssueService issueService;

	@PostMapping
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueResponse>> createIssue(
		@PathVariable String workspaceCode,
		@RequestBody @Valid CreateIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.createIssue(request.toCommand(workspaceCode));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Issue created.", response));
	}

	@PatchMapping("/{issueKey}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueResponse> updateIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.updateIssue(request.toCommand(workspaceCode, issueKey));
		return ApiResponse.ok("Issue updated.", response);
	}

	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{issueKey}/status")
	// public ApiResponse<IssueResponse> updateIssueStatus(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String issueKey,
	// 	@CurrentMember MemberUserDetails userDetails,
	// 	@RequestBody @Valid UpdateIssueStatusRequest request
	// ) {
	// 	IssueResponse response = issueCommandService.updateIssueStatus(
	// 		workspaceCode,
	// 		issueKey,
	// 		userDetails.getMemberId(),
	// 		request
	// 	);
	//
	// 	return ApiResponse.ok("Issue status updated.", response);
	// }

	@PatchMapping("/{issueKey}/parent")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueResponse> assignParentIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid AssignParentIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.assignParentIssue(request.toCommand(workspaceCode, issueKey));
		return ApiResponse.ok("Parent issue assigned.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> removeParentIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.removeParentIssue(
			workspaceCode,
			issueKey,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Parent issue relationship removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PostMapping("{issueKey}/watch")
	public ApiResponse<IssueResponse> watchIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.watchIssue(
			workspaceCode,
			issueKey,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Watching issue.", response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@DeleteMapping("{issueKey}/watch")
	public ApiResponse<IssueResponse> unwatchIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.unwatchIssue(
			workspaceCode,
			issueKey,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Unwatched issue.", response);
	}
}
