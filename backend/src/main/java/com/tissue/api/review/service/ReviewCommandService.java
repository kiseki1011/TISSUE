package com.tissue.api.review.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.domain.Review;
import com.tissue.api.review.domain.enums.ReviewStatus;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.review.exception.ReviewNotFoundException;
import com.tissue.api.review.presentation.dto.request.CreateReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewStatusRequest;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.CreateReviewResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RequestReviewResponse;
import com.tissue.api.review.presentation.dto.response.UpdateReviewResponse;
import com.tissue.api.review.presentation.dto.response.UpdateReviewStatusResponse;
import com.tissue.api.review.validator.ReviewValidator;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Todo
 *  - currentReviewRound가 아닌 리뷰에 대해서는 수정 불가하도록 검증 로직 추가
 *  - 리뷰어 등록/해제는 해당 이슈의 assignees에 포함되어 있어야 가능하도록 검증 로직 추가
 *    - 리뷰어로 등록된 경우, 본인이 리뷰어 해제하는 것도 허용
 *  - 리뷰 업데이트는 해당 리뷰를 작성한 리뷰어만 가능하도록 검증 로직 추가
 *  - 리뷰 삭제는 해당 리뷰를 작성한 리뷰어만 가능하도록 검증 로직 추가
 *  - 알림 서비스 구현 필요
 *    - 리뷰 상태 변경 -> 모든 리뷰가 작성되었고 전부 APPROVED -> 이슈 assignees에게 알림
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReviewCommandService {

	private final ReviewRepository reviewRepository;
	private final IssueRepository issueRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final ReviewValidator reviewValidator;

	@Transactional
	public AddReviewerResponse addReviewer(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId
	) {
		Issue issue = findIssue(workspaceCode, issueKey);
		WorkspaceMember reviewer = findWorkspaceMember(reviewerWorkspaceMemberId, workspaceCode);

		reviewValidator.validateRoleIsLowerThanMember(reviewer);
		// Todo: 요청자가 이슈 assignee에 포함되는지 검증 추가(assignee 구현하고 나서)
		//  - currentWorkspaceMemberId를 컨트롤러에서 받아와서 사용

		issue.addReviewer(reviewer);

		return AddReviewerResponse.from(reviewer);
	}

	@Transactional
	public RemoveReviewerResponse removeReviewer(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = findIssue(workspaceCode, issueKey);

		WorkspaceMember reviewer = findWorkspaceMember(reviewerWorkspaceMemberId, workspaceCode);

		// Todo: validateRequesterIsAssignee or requesterIsReviewer 추가
		issue.removeReviewer(reviewer);

		return RemoveReviewerResponse.from(reviewer, issue);
	}

	/*
	 * Todo
	 *  - 요청자가 이슈 assignee에 포함되는지 검증 추가(assignee 구현하고 나서)
	 *  - 아니면 assignee에 포함되는지 인터셉터에서 확인하도록 하는 방법도 있지만, 별로 인듯 -> 로직 추적하기가 어려움
	 *  - currentWorkspaceMemberId를 컨트롤러에서 받아와서 사용
	 */
	@Transactional
	public RequestReviewResponse requestReview(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = findIssue(workspaceCode, issueKey);

		// Todo
		// WorkspaceMember requester = findWorkspaceMember(requesterId, workspaceCode);
		// reviewValidator.validateRequester(requester);

		issue.requestReview();

		return RequestReviewResponse.from(issue);
	}

	@Transactional
	public CreateReviewResponse createReview(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId,
		CreateReviewRequest request
	) {
		Issue issue = findIssue(workspaceCode, issueKey);

		IssueReviewer issueReviewer = reviewValidator.validateIsReviewerAndGet(reviewerWorkspaceMemberId, issue);
		reviewValidator.validateReviewIsCreateable(issue);

		Review review = issueReviewer.addReview(
			request.status(),
			request.title(),
			request.content(),
			issue.getCurrentReviewRound()
		);

		Review savedReview = reviewRepository.save(review);

		updateIssueStatusBasedOnReview(issue, request.status());

		return CreateReviewResponse.from(savedReview);
	}

	@Transactional
	public UpdateReviewResponse updateReview(
		Long reviewId,
		Long reviewerWorkspaceMemberId,
		UpdateReviewRequest request
	) {
		Review review = findReview(reviewId);

		reviewValidator.validateReviewOwnership(review, reviewerWorkspaceMemberId);

		review.updateTitle(request.title());
		review.updateContent(request.content());

		return UpdateReviewResponse.from(review);
	}

	@Transactional
	public UpdateReviewStatusResponse updateReviewStatus(
		String workspaceCode,
		String issueKey,
		Long reviewId,
		Long requesterWorkspaceMemberId,
		UpdateReviewStatusRequest request
	) {
		Issue issue = findIssue(workspaceCode, issueKey);
		Review review = findReview(reviewId);

		reviewValidator.validateReviewOwnership(review, requesterWorkspaceMemberId);

		review.updateStatus(request.status());
		updateIssueStatusBasedOnReview(issue, request.status());

		return UpdateReviewStatusResponse.from(review);
	}

	private Issue findIssue(String code, String issueKey) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, code)
			.orElseThrow(IssueNotFoundException::new);
	}

	private WorkspaceMember findWorkspaceMember(Long id, String code) {
		return workspaceMemberRepository.findByIdAndWorkspaceCode(id, code)
			.orElseThrow(WorkspaceMemberNotFoundException::new);
	}

	private Review findReview(Long id) {
		return reviewRepository.findById(id)
			.orElseThrow(ReviewNotFoundException::new);
	}

	private void updateIssueStatusBasedOnReview(Issue issue, ReviewStatus reviewStatus) {
		// CHANGES_REQUESTED의 경우 자동 상태 변경
		if (reviewStatus == ReviewStatus.CHANGES_REQUESTED) {
			issue.updateStatus(IssueStatus.CHANGES_REQUESTED);
			return;
		}

		/*
		 * Todo
		 *  - 모든 리뷰어가 승인한 경우, 작업자에게 알림
		 *  - 알림 서비스 구현 필요
		 *  - 자동으로 이슈 상태 DONE으로 변경 X
		 */
		boolean allApproved = issue.getReviewers().stream()
			.allMatch(reviewer ->
				reviewer.getCurrentReviewStatus(issue.getCurrentReviewRound())
					== ReviewStatus.APPROVED
			);

		if (allApproved) {
			// Todo: 알림 서비스 구현 후 추가
		}
	}
}
