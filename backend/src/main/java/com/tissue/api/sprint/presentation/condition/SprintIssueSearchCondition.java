package com.tissue.api.sprint.presentation.condition;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.issue.domain.enums.IssuePriority;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SprintIssueSearchCondition(
	// List<IssueStatus> statuses,
	// List<IssueType> types,
	List<IssuePriority> priorities,
	@Size(min = 2, message = "{valid.size.keyword}")
	String keyword
) {
	public SprintIssueSearchCondition {
		// if (statuses == null || statuses.isEmpty()) {
		// 	statuses = List.of(IssueStatus.TODO, IssueStatus.IN_PROGRESS);
		// }
		// if (types == null) {
		// 	types = new ArrayList<>();
		// }
		if (priorities == null) {
			priorities = new ArrayList<>();
		}
		// 빈 문자열이나 공백만 있는 경우 null로 처리
		if (keyword != null && keyword.trim().isEmpty()) {
			keyword = null;
		}
	}

	public SprintIssueSearchCondition() {
		this(
			// List.of(IssueStatus.TODO, IssueStatus.IN_PROGRESS),
			// new ArrayList<>(),
			new ArrayList<>(),
			null
		);
	}

	// 키워드 검색 조건 있는지 확인하는 유틸리티 메서드
	public boolean hasKeyword() {
		return keyword != null && !keyword.trim().isEmpty();
	}
}
