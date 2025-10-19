package com.tissue.api.issue.domain.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.model.Issue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@Getter
@RequiredArgsConstructor
public class IssuePolicy {

	@Value("${issue.max-reviewers:10}")
	private final int maxReviewers;

	public void ensureCanAddReviewer(int currentCount) {
		if (currentCount >= maxReviewers) {
			throw new InvalidOperationException("The max number of reviewers is " + maxReviewers);
		}
	}

	public void ensureCanAddReviewer(Issue issue) {
		if (issue.getReviewers().size() >= maxReviewers) {
			throw new InvalidOperationException("The max number of reviewers is " + maxReviewers);
		}
	}
}
