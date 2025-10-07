package com.tissue.api.review.presentation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/reviews")
public class ReviewController {

	// private final ReviewCommandService reviewCommandService;

	/*
	 * Todo
	 *  - 리뷰어들에게 리뷰 요청
	 *  - 리뷰 상태 변경
	 *  - 리뷰 삭제(내꺼)
	 *  - 리뷰 삭제(MANAGER 권한 이상) - 위의 리뷰 삭제와 서비스 같이 사용
	 *  - 리뷰 댓글 달기(CommentController에서 진행하는 것이 좋을까?)
	 */

	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @ResponseStatus(HttpStatus.CREATED)
	// @PostMapping
	// public ApiResponse<ReviewResponse> submitReview(
	// 	@PathVariable String workspaceKey,
	// 	@PathVariable String issueKey,
	// 	@RequestBody @Valid SubmitReviewRequest request,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	ReviewResponse response = reviewCommandService.submitReview(
	// 		workspaceKey,
	// 		issueKey,
	// 		userDetails.getMemberId(),
	// 		request
	// 	);
	//
	// 	return ApiResponse.ok("Review submitted.", response);
	// }
	//
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{reviewId}")
	// public ApiResponse<ReviewResponse> updateReview(
	// 	@PathVariable String workspaceKey,
	// 	@PathVariable Long reviewId,
	// 	@RequestBody @Valid UpdateReviewRequest request,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	ReviewResponse response = reviewCommandService.updateReview(
	// 		workspaceKey,
	// 		reviewId,
	// 		userDetails.getMemberId(),
	// 		request
	// 	);
	//
	// 	return ApiResponse.ok("Review updated.", response);
	// }

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
