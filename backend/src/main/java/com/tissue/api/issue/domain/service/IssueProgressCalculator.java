package com.tissue.api.issue.domain.service;

import com.tissue.api.issue.domain.model.Issue;

public interface IssueProgressCalculator {

	/**
	 * 이슈의 진행도를 계산
	 */
	Integer calculate(Issue issue);

	/**
	 * 이 계산기가 해당 이슈에 적용 가능한지 여부
	 */
	boolean supports(Issue issue);

	/**
	 * 진행도 타입
	 */
	ProgressType getType();
}
