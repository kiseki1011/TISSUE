package com.tissue.api.sprint.presentation.condition;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;

public record SprintIssueSearchCondition(
	List<IssueStatus> statuses,
	List<IssueType> types,
	List<IssuePriority> priorities
) {
	public SprintIssueSearchCondition {
		if (statuses == null || statuses.isEmpty()) {
			statuses = List.of(IssueStatus.TODO, IssueStatus.IN_PROGRESS);
		}
		if (types == null) {
			types = new ArrayList<>();
		}
		if (priorities == null) {
			priorities = new ArrayList<>();
		}
	}

	public SprintIssueSearchCondition() {
		this(
			List.of(IssueStatus.TODO, IssueStatus.IN_PROGRESS),
			new ArrayList<>(),
			new ArrayList<>()
		);
	}
}
