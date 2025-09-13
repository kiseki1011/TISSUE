package com.tissue.api.comment.presentation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/reviews/{reviewId}/comments")
public class ReviewCommentController {

	/*
	 * Todo
	 *  - 댓글 조회
	 *  - 깃허브 PR 연동
	 */

	// private final ReviewCommentCommandService reviewCommentCommandService;
	//
	// @PostMapping
	// @ResponseStatus(HttpStatus.CREATED)
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<ReviewCommentResponse> createComment(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String issueKey,
	// 	@PathVariable Long reviewId,
	// 	@Valid @RequestBody CreateReviewCommentRequest request,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	ReviewCommentResponse response = reviewCommentCommandService.createComment(
	// 		workspaceCode,
	// 		issueKey,
	// 		reviewId,
	// 		request,
	// 		userDetails.getMemberId()
	// 	);
	//
	// 	return ApiResponse.created("Comment created.", response);
	// }
	//
	// @PatchMapping("/{commentId}")
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<ReviewCommentResponse> updateComment(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String issueKey,
	// 	@PathVariable Long reviewId,
	// 	@PathVariable Long commentId,
	// 	@Valid @RequestBody UpdateReviewCommentRequest request,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	ReviewCommentResponse response = reviewCommentCommandService.updateComment(
	// 		workspaceCode,
	// 		issueKey,
	// 		reviewId,
	// 		commentId,
	// 		request,
	// 		userDetails.getMemberId()
	// 	);
	//
	// 	return ApiResponse.ok("Comment updated.", response);
	// }
	//
	// /**
	//  * Todo
	//  *  - 만약 깃허브 API와 연동을 하는 경우 댓글 삭제 후 복구는 불가능 하도록 만들어야 하나?
	//  *  - 깃허브 API의 댓글 시스템 파악이 필요
	//  */
	// @DeleteMapping("/{commentId}")
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<ReviewCommentResponse> deleteComment(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String issueKey,
	// 	@PathVariable Long reviewId,
	// 	@PathVariable Long commentId,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	ReviewCommentResponse response = reviewCommentCommandService.deleteComment(
	// 		workspaceCode,
	// 		issueKey,
	// 		reviewId,
	// 		commentId,
	// 		userDetails.getMemberId()
	// 	);
	//
	// 	return ApiResponse.ok("Comment deleted.", response);
	// }
}
