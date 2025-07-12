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

	private final IssueCommandService issueCommandService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<IssueResponse> createIssue(
		@PathVariable String workspaceCode,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid CreateIssueRequest request
	) {
		IssueResponse response = issueCommandService.createIssue(workspaceCode, userDetails.getMemberId(), request);

		return ApiResponse.created("Issue created.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/status")
	public ApiResponse<IssueResponse> updateIssueStatus(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid UpdateIssueStatusRequest request
	) {
		IssueResponse response = issueCommandService.updateIssueStatus(
			workspaceCode,
			issueKey,
			userDetails.getMemberId(),
			request
		);

		return ApiResponse.ok("Issue status updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}")
	public ApiResponse<IssueResponse> updateIssueDetail(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid UpdateIssueRequest request
	) {
		IssueResponse response = issueCommandService.updateIssue(
			workspaceCode,
			issueKey,
			userDetails.getMemberId(),
			request
		);

		return ApiResponse.ok("Issue details updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> assignParentIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid AddParentIssueRequest request
	) {
		IssueResponse response = issueCommandService.assignParentIssue(
			workspaceCode,
			issueKey,
			userDetails.getMemberId(),
			request
		);

		return ApiResponse.ok("Parent issue assigned.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> removeParentIssue(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCommandService.removeParentIssue(
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
		IssueResponse response = issueCommandService.watchIssue(
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
		IssueResponse response = issueCommandService.unwatchIssue(
			workspaceCode,
			issueKey,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Unwatched issue.", response);
	}
}
