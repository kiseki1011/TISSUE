package com.tissue.api.issue.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.IssueReviewerService;
import com.tissue.api.issue.presentation.dto.request.AddReviewerRequest;
import com.tissue.api.issue.presentation.dto.request.RemoveReviewerRequest;
import com.tissue.api.issue.presentation.dto.response.IssueReviewerResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO: Move to IssueAssociateController
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/reviewers")
public class IssueReviewerController {

	private final IssueReviewerService issueReviewerService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<IssueReviewerResponse> addReviewer(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid AddReviewerRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueReviewerResponse response = issueReviewerService.addReviewer(
			workspaceCode,
			issueKey,
			request.toCommand(),
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Reviewer added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping
	public ApiResponse<IssueReviewerResponse> removeReviewer(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveReviewerRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueReviewerResponse response = issueReviewerService.removeReviewer(
			workspaceCode,
			issueKey,
			request.toCommand(),
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Reviewer removed.", response);
	}

	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PostMapping("/reviews")
	// public ApiResponse<IssueResponse> requestReview(
	// 	@PathVariable String workspaceKey,
	// 	@PathVariable String issueKey,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	IssueResponse response = issueReviewerCommandService.requestReview(
	// 		workspaceKey,
	// 		issueKey,
	// 		userDetails.getMemberId()
	// 	);
	//
	// 	return ApiResponse.ok("Requested review for issue.", response);
	// }
}
