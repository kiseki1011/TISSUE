package com.tissue.comment.presentation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.comment.application.service.IssueCommentCommandService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/comments")
public class IssueCommentController {

	/*
	 * Todo
	 *  - 댓글 작성(해당 워크스페이스에서 VIEWER 이상이면 누구나 가능)
	 *  - 댓글 수정
	 *    - 작성자 가능
	 *    - MANAGER 권한 이상 가능
	 *  - 댓글 삭제
	 * 	  - 작성자 가능
	 *    - MANAGER 권한 이상 가능
	 *    - soft delete 사용(ACTIVE -> DELETED)
	 *  - 댓글 조회
	 *    - 내가 해당 워크스페이스에서 작성한 모든 이슈 댓글 조회
	 *    - 특정 이슈의 모든 댓글 조회
	 *    - 특정 댓글의 모든 대댓글 조회
	 */

	// TODO(seungki1011, 2025-04-11): comment revert api

	private final IssueCommentCommandService issueCommentCommandService;

	// @PostMapping
	// @ResponseStatus(HttpStatus.CREATED)
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<IssueCommentResponse> createComment(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String issueKey,
	// 	@Valid @RequestBody CreateIssueCommentRequest request,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	IssueCommentResponse response = issueCommentCommandService.createComment(
	// 		workspaceCode,
	// 		issueKey,
	// 		request,
	// 		userDetails.getMemberId()
	// 	);
	//
	// 	return ApiResponse.created("Comment created.", response);
	// }
	//
	// @PatchMapping("/{commentId}")
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<IssueCommentResponse> updateComment(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String issueKey,
	// 	@PathVariable Long commentId,
	// 	@Valid @RequestBody UpdateIssueCommentRequest request,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	IssueCommentResponse response = issueCommentCommandService.updateComment(
	// 		workspaceCode,
	// 		issueKey,
	// 		commentId,
	// 		request,
	// 		userDetails.getMemberId()
	// 	);
	//
	// 	return ApiResponse.ok("Comment updated.", response);
	// }
	//
	// @DeleteMapping("/{commentId}")
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<IssueCommentResponse> deleteComment(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String issueKey,
	// 	@PathVariable Long commentId,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	IssueCommentResponse response = issueCommentCommandService.deleteComment(
	// 		workspaceCode,
	// 		issueKey,
	// 		commentId,
	// 		userDetails.getMemberId()
	// 	);
	//
	// 	return ApiResponse.ok("Comment deleted.", response);
	// }

}
