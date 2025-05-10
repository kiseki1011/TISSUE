package com.tissue.api.comment.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.comment.presentation.dto.request.CreateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.response.ReviewCommentResponse;
import com.tissue.api.comment.service.command.ReviewCommentCommandService;
import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceCode}/issues/{issueKey}/reviews/{reviewId}/comments")
public class ReviewCommentController {

	private final ReviewCommentCommandService reviewCommentCommandService;

	/*
	 * Todo
	 *  - 댓글 조회
	 *  - 깃허브 PR 연동
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<ReviewCommentResponse> createComment(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@PathVariable Long reviewId,
		@Valid @RequestBody CreateReviewCommentRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		ReviewCommentResponse response = reviewCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			reviewId,
			request,
			loginMemberId
		);

		return ApiResponse.created("Comment created.", response);
	}

	@PatchMapping("/{commentId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<ReviewCommentResponse> updateComment(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@PathVariable Long reviewId,
		@PathVariable Long commentId,
		@Valid @RequestBody UpdateReviewCommentRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		ReviewCommentResponse response = reviewCommentCommandService.updateComment(
			workspaceCode,
			issueKey,
			reviewId,
			commentId,
			request,
			loginMemberId
		);

		return ApiResponse.ok("Comment updated.", response);
	}

	/**
	 * Todo
	 *  - 만약 깃허브 API와 연동을 하는 경우 댓글 삭제 후 복구는 불가능 하도록 만들어야 하나?
	 *  - 깃허브 API의 댓글 시스템 파악이 필요
	 */
	@DeleteMapping("/{commentId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<ReviewCommentResponse> deleteComment(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@PathVariable Long reviewId,
		@PathVariable Long commentId,
		@ResolveLoginMember Long loginMemberId
	) {
		ReviewCommentResponse response = reviewCommentCommandService.deleteComment(
			workspaceCode,
			issueKey,
			reviewId,
			commentId,
			loginMemberId
		);

		return ApiResponse.ok("Comment deleted.", response);
	}
}
