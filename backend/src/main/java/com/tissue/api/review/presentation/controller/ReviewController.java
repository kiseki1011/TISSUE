package com.tissue.api.review.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.review.presentation.dto.request.CreateReviewRequest;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.CreateReviewResponse;
import com.tissue.api.review.service.ReviewCommandService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/issues/{issueKey}")
public class ReviewController {

	private final ReviewCommandService reviewCommandService;

	/*
	 * Todo
	 *  - 리뷰어 등록
	 *  - 리뷰어들에게 리뷰 요청
	 *    - 알림 보내기(추후에 Notification 도메인과 함께 개발)
	 *    - 이벤트 기반 아키텍쳐 사용?
	 *  - 리뷰 작성/등록
	 *  - 리뷰 수정
	 *  - 리뷰 상태 변경
	 *    - 이벤트 기반 아키텍쳐 사용?
	 *  - 리뷰 삭제
	 *  - 리뷰 댓글 달기(CommentController에서 진행하는 것이 좋을까?)
	 */

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.MEMBER)
	@PostMapping("/reviewers/{reviewerId}")
	public ApiResponse<AddReviewerResponse> addReviewer(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable Long reviewerId
	) {
		AddReviewerResponse response = reviewCommandService.addReviewer(
			code,
			issueKey,
			reviewerId
		);

		return ApiResponse.ok("Reviewer added.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.MEMBER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/reviews")
	public ApiResponse<CreateReviewResponse> createReview(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long reviewerId,
		@RequestBody @Valid CreateReviewRequest request
	) {
		CreateReviewResponse response = reviewCommandService.createReview(
			code,
			issueKey,
			reviewerId,
			request
		);

		return ApiResponse.ok("Review created.", response);
	}
}
