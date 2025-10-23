package com.tissue.api.workflow.domain.gaurd;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.model.Issue;

@Component
public class NotBlockedGuard implements TransitionGuard {

	@Override
	public boolean evaluate(GuardContext context) {
		Issue issue = context.getIssue();

		// Blocked by 관계 확인
		List<Issue> blockingIssues = issue.getRelations().getBlockedByIssues();

		if (blockingIssues.isEmpty()) {
			return true;  // 차단하는 이슈 없음
		}

		// 모든 blocking 이슈가 terminal 상태인지 확인
		return blockingIssues.stream()
			.allMatch(blocking -> blocking.getCurrentState().isTerminal());
	}

	@Override
	public String getFailureMessage(GuardContext context) {
		Issue issue = context.getIssue();

		List<Issue> unresolved = issue.getRelations().getBlockedByIssues().stream()
			.filter(blocking -> !blocking.getCurrentState().isTerminal())
			.toList();

		String blockingKeys = unresolved.stream()
			.map(Issue::getKey)
			.collect(Collectors.joining(", "));

		return "This issue is blocked by: " + blockingKeys + ". Resolve blocking issues first.";
	}

	@Override
	public GuardType getType() {
		return GuardType.NOT_BLOCKED;
	}
}
