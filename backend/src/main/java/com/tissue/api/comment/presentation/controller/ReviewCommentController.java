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

import com.tissue.api.comment.application.service.command.ReviewCommentCommandService;
import com.tissue.api.comment.presentation.dto.request.CreateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.response.ReviewCommentResponse;
import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/reviews/{reviewId}/comments")
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
		@CurrentMember MemberUserDetails userDetails
	) {
		ReviewCommentResponse response = reviewCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			reviewId,
			request,
			userDetails.getMemberId()
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
		@CurrentMember MemberUserDetails userDetails
	) {
		ReviewCommentResponse response = reviewCommentCommandService.updateComment(
			workspaceCode,
			issueKey,
			reviewId,
			commentId,
			request,
			userDetails.getMemberId()
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
		@CurrentMember MemberUserDetails userDetails
	) {
		ReviewCommentResponse response = reviewCommentCommandService.deleteComment(
			workspaceCode,
			issueKey,
			reviewId,
			commentId,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Comment deleted.", response);
	}
}
