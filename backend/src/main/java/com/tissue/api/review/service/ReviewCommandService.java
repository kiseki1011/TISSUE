package com.tissue.api.review.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.validator.IssueReviewerValidator;
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
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final IssueReviewerValidator issueReviewerValidator;

	@Transactional
	public AddReviewerResponse addReviewer(
		String workspaceCode,
		String issueKey,
		Long reviewerId
	) {
		Issue issue = findIssue(workspaceCode, issueKey);
		WorkspaceMember reviewer = findReviewer(reviewerId);

		issueReviewerValidator.validateReviewer(reviewer);

		issue.addReviewer(reviewer);

		return AddReviewerResponse.from(reviewer);
	}

	private Issue findIssue(String code, String issueKey) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, code)
			.orElseThrow(IssueNotFoundException::new);
	}

	private WorkspaceMember findReviewer(Long id) {
		return workspaceMemberRepository.findById(id)
			.orElseThrow(WorkspaceMemberNotFoundException::new);
	}
}
