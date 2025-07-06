package com.tissue.api.review.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.review.application.service.command.ReviewCommandService;
import com.tissue.api.review.presentation.dto.request.SubmitReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewRequest;
import com.tissue.api.review.presentation.dto.response.ReviewResponse;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/issues/{issueKey}/reviews")
public class ReviewController {

	private final ReviewCommandService reviewCommandService;

	/*
	 * Todo
	 *  - 리뷰어들에게 리뷰 요청
	 *  - 리뷰 상태 변경
	 *  - 리뷰 삭제(내꺼)
	 *  - 리뷰 삭제(MANAGER 권한 이상) - 위의 리뷰 삭제와 서비스 같이 사용
	 *  - 리뷰 댓글 달기(CommentController에서 진행하는 것이 좋을까?)
	 */

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<ReviewResponse> submitReview(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid SubmitReviewRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		ReviewResponse response = reviewCommandService.submitReview(
			workspaceCode,
			issueKey,
			loginMemberId,
			request
		);

		return ApiResponse.ok("Review submitted.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{reviewId}")
	public ApiResponse<ReviewResponse> updateReview(
		@PathVariable String workspaceCode,
		@PathVariable Long reviewId,
		@RequestBody @Valid UpdateReviewRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		ReviewResponse response = reviewCommandService.updateReview(
			workspaceCode,
			reviewId,
			loginMemberId,
			request
		);

		return ApiResponse.ok("Review updated.", response);
	}

	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{reviewId}/status")
	// public ApiResponse<ReviewResponse> updateReviewStatus(
	// 	@PathVariable String code,
	// 	@PathVariable String issueKey,
	// 	@PathVariable Long reviewId,
	// 	@CurrentWorkspaceMember Long requesterId,
	// 	@RequestBody @Valid UpdateReviewStatusRequest request
	// ) {
	// 	ReviewResponse response = reviewCommandService.updateReviewStatus(
	// 		code,
	// 		issueKey,
	// 		reviewId,
	// 		requesterId,
	// 		request
	// 	);
	//
	// 	return ApiResponse.ok("Review status updated.", response);
	// }
}
