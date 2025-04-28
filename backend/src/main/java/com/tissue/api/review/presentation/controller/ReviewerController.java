package com.tissue.api.review.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.review.presentation.dto.request.AddReviewerRequest;
import com.tissue.api.review.presentation.dto.request.RemoveReviewerRequest;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RequestReviewResponse;
import com.tissue.api.review.service.command.ReviewerCommandService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/issues/{issueKey}/reviewers")
public class ReviewerController {

	private final ReviewerCommandService reviewerCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<AddReviewerResponse> addReviewer(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid AddReviewerRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		AddReviewerResponse response = reviewerCommandService.addReviewer(
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
	public ApiResponse<RemoveReviewerResponse> removeReviewer(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveReviewerRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		RemoveReviewerResponse response = reviewerCommandService.removeReviewer(
			workspaceCode,
			issueKey,
			request.toCommand(),
			loginMemberId
		);

		return ApiResponse.ok("Reviewer removed.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/reviews")
	public ApiResponse<RequestReviewResponse> requestReview(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@ResolveLoginMember Long loginMemberId
	) {
		RequestReviewResponse response = reviewerCommandService.requestReview(
			workspaceCode,
			issueKey,
			loginMemberId
		);

		return ApiResponse.ok("Requested review for issue.", response);
	}
}
