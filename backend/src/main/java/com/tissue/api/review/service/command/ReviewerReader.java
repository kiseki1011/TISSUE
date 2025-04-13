package com.tissue.api.review.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.domain.repository.IssueReviewerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewerReader {

	private final IssueReviewerRepository issueReviewerRepository;

	public IssueReviewer findByIssueKeyAndWorkspaceMemberId(
		String issueKey,
		Long workspaceMemberId
	) {
		return issueReviewerRepository.findByIssueKeyAndReviewerId(issueKey, workspaceMemberId)
			.orElseThrow(() -> new InvalidOperationException(String.format(
				"Must be a reviewer to create a review. issueKey: %s, workspaceMemberId: %d",
				issueKey, workspaceMemberId))
			);
	}
}
