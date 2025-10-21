package com.tissue.api.issue.domain.service;

import com.tissue.api.issue.domain.model.Issue;

public interface IssueProgressCalculator {

	default Integer calculate(Issue issue) {
		boolean doesNotSupport = !supports(issue);
		if (doesNotSupport) {
			return null;
		}
		return doCalculate(issue);
	}

	Integer doCalculate(Issue issue);

	/**
	 * 진행도 계산 전략이 해당 이슈에 적용 가능한지 여부
	 */
	boolean supports(Issue issue);

	ProgressType getType();
}
