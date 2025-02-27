package com.tissue.api.review.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RequestReviewResponse;
import com.tissue.api.review.service.command.ReviewerCommandService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMember;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/issues/{issueKey}/reviewers")
public class ReviewerController {

	private final ReviewerCommandService reviewerCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{workspaceMemberId}")
	public ApiResponse<AddReviewerResponse> addReviewer(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable Long workspaceMemberId,
		@CurrentWorkspaceMember Long requesterId
	) {
		AddReviewerResponse response = reviewerCommandService.addReviewer(
			code,
			issueKey,
			workspaceMemberId,
			requesterId
		);

		return ApiResponse.ok("Reviewer added.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{workspaceMemberId}")
	public ApiResponse<RemoveReviewerResponse> removeReviewer(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable Long workspaceMemberId,
		@CurrentWorkspaceMember Long requesterId
	) {
		RemoveReviewerResponse response = reviewerCommandService.removeReviewer(
			code,
			issueKey,
			workspaceMemberId,
			requesterId
		);

		return ApiResponse.ok("Reviewer removed.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<RequestReviewResponse> requestReview(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long requesterId
	) {
		RequestReviewResponse response = reviewerCommandService.requestReview(
			code,
			issueKey,
			requesterId
		);

		return ApiResponse.ok("Requested reviewing for issue.", response);
	}
}
