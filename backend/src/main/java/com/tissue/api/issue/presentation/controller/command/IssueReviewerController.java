package com.tissue.api.issue.presentation.controller.command;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.command.IssueReviewerCommandService;
import com.tissue.api.issue.presentation.controller.dto.request.AddReviewerRequest;
import com.tissue.api.issue.presentation.controller.dto.request.RemoveReviewerRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueResponse;
import com.tissue.api.issue.presentation.controller.dto.response.IssueReviewerResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/issues/{issueKey}/reviewers")
public class IssueReviewerController {

	private final IssueReviewerCommandService issueReviewerCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<IssueReviewerResponse> addReviewer(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid AddReviewerRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		IssueReviewerResponse response = issueReviewerCommandService.addReviewer(
			workspaceCode,
			issueKey,
			request.toCommand(),
			loginMemberId
		);

		return ApiResponse.ok("Reviewer added.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping
	public ApiResponse<IssueReviewerResponse> removeReviewer(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveReviewerRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		IssueReviewerResponse response = issueReviewerCommandService.removeReviewer(
			workspaceCode,
			issueKey,
			request.toCommand(),
			loginMemberId
		);

		return ApiResponse.ok("Reviewer removed.", response);
	}

	// TODO: IssueController로 이동하는게 맞지 않을까?
	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/reviews")
	public ApiResponse<IssueResponse> requestReview(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@ResolveLoginMember Long loginMemberId
	) {
		IssueResponse response = issueReviewerCommandService.requestReview(
			workspaceCode,
			issueKey,
			loginMemberId
		);

		return ApiResponse.ok("Requested review for issue.", response);
	}
}
