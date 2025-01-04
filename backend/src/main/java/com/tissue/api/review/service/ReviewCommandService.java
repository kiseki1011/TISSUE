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
import com.tissue.api.review.domain.repository.IssueReviewerRepository;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.review.exception.NotIssueReviewerException;
import com.tissue.api.review.presentation.dto.request.CreateReviewRequest;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.CreateReviewResponse;
import com.tissue.api.review.validator.ReviewValidator;
import com.tissue.api.review.validator.ReviewerValidator;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReviewCommandService {

	private final ReviewRepository reviewRepository;
	private final IssueRepository issueRepository;
	private final IssueReviewerRepository issueReviewerRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final ReviewerValidator reviewerValidator;
	private final ReviewValidator reviewValidator;

	@Transactional
	public AddReviewerResponse addReviewer(
		String workspaceCode,
		String issueKey,
		Long reviewerId
	) {
		Issue issue = findIssue(workspaceCode, issueKey);
		WorkspaceMember reviewer = findReviewer(reviewerId, workspaceCode);

		reviewerValidator.validateReviewer(reviewer);

		issue.addReviewer(reviewer);

		return AddReviewerResponse.from(reviewer);
	}

	@Transactional
	public CreateReviewResponse createReview(
		String workspaceCode,
		String issueKey,
		Long reviewerId,
		CreateReviewRequest request
	) {
		Issue issue = findIssue(workspaceCode, issueKey);

		IssueReviewer issueReviewer = issue.getReviewers().stream()
			.filter(reviewer -> reviewer.getReviewer().getId().equals(reviewerId))
			.findFirst()
			.orElseThrow(NotIssueReviewerException::new);

		reviewValidator.validateReviewIsCreateable(issue);

		Review review = issueReviewer.addReview(
			request.status(),
			request.content(),
			issue.getCurrentReviewRound()
		);

		updateIssueStatusBasedOnReview(issue, request.status());

		return CreateReviewResponse.from(review);
	}

	private Issue findIssue(String code, String issueKey) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, code)
			.orElseThrow(IssueNotFoundException::new);
	}

	private WorkspaceMember findReviewer(Long id, String code) {
		return workspaceMemberRepository.findByIdAndWorkspaceCode(id, code)
			.orElseThrow(WorkspaceMemberNotFoundException::new);
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
