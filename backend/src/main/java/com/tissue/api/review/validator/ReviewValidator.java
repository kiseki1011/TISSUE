package com.tissue.api.review.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.review.exception.IssueStatusNotInReviewException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewValidator {

	public void validateReviewIsCreateable(Issue issue) {
		if (issue.getStatus() != IssueStatus.IN_REVIEW) {
			throw new IssueStatusNotInReviewException(); // Todo: InvalidIssueStatusException로 변경(메세지로 세부 사항 전달)
		}
	}
}
