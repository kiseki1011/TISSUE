package com.tissue.api.review.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.review.presentation.dto.request.CreateReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewStatusRequest;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.CreateReviewResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RequestReviewResponse;
import com.tissue.api.review.presentation.dto.response.UpdateReviewResponse;
import com.tissue.api.review.presentation.dto.response.UpdateReviewStatusResponse;
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
	 *  - 리뷰 삭제(내꺼)
	 *  - 리뷰 삭제(MANAGER 권한 이상) - 위의 리뷰 삭제와 서비스 같이 사용
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
	@DeleteMapping("/reviewers/{reviewerId}")
	public ApiResponse<RemoveReviewerResponse> removeReviewer(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable Long reviewerId,
		@CurrentWorkspaceMember Long requesterId
	) {
		RemoveReviewerResponse response = reviewCommandService.removeReviewer(
			code,
			issueKey,
			reviewerId,
			requesterId
		);

		return ApiResponse.ok("Reviewer removed.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.MEMBER)
	@PostMapping("/reviewers")
	public ApiResponse<RequestReviewResponse> requestReview(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long requesterId
	) {
		RequestReviewResponse response = reviewCommandService.requestReview(code, issueKey, requesterId);

		return ApiResponse.ok("Requested reviewing for issue.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.MEMBER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/reviews")
	public ApiResponse<CreateReviewResponse> createReview(
		@PathVariable String code,
		@PathVariable String issueKey,
		@CurrentWorkspaceMember Long requesterId,
		@RequestBody @Valid CreateReviewRequest request
	) {
		CreateReviewResponse response = reviewCommandService.createReview(
			code,
			issueKey,
			requesterId,
			request
		);

		return ApiResponse.ok("Review created.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.MEMBER)
	@PatchMapping("/reviews/{reviewId}")
	public ApiResponse<UpdateReviewResponse> updateReview(
		@PathVariable Long reviewId,
		@CurrentWorkspaceMember Long requesterId,
		@RequestBody @Valid UpdateReviewRequest request
	) {
		UpdateReviewResponse response = reviewCommandService.updateReview(
			reviewId,
			requesterId,
			request
		);

		return ApiResponse.ok("Review updated.", response);
	}

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.MEMBER)
	@PatchMapping("/reviews/{reviewId}/status")
	public ApiResponse<UpdateReviewStatusResponse> updateReviewStatus(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable Long reviewId,
		@CurrentWorkspaceMember Long requesterId,
		@RequestBody @Valid UpdateReviewStatusRequest request
	) {
		UpdateReviewStatusResponse response = reviewCommandService.updateReviewStatus(
			code,
			issueKey,
			reviewId,
			requesterId,
			request
		);

		return ApiResponse.ok("Review status updated.", response);
	}
}
