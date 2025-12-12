package com.tissue.api.workflow.domain.guard;

import static com.tissue.api.workflow.domain.enums.StateCategory.*;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;

@Component
public class NotBlockedGuard implements TransitionGuard {

	@Override
	public boolean evaluate(GuardContext context) {
		Issue issue = context.getIssue();

		List<Issue> blockingIssues = issue.getRelations().getBlockedByIssues();

		if (blockingIssues.isEmpty()) {
			return true;
		}

		return blockingIssues.stream()
			.allMatch(blocking -> blocking.getCurrentState().isCategorizedAs(DONE));
	}

	@Override
	public String getFailureMessage(GuardContext context) {
		Issue issue = context.getIssue();

		List<Issue> unresolved = issue.getRelations().getBlockedByIssues().stream()
			.filter(blocking -> !blocking.getCurrentState().isCategorizedAs(DONE))
			.toList();

		String blockingKeys = unresolved.stream()
			.map(Issue::getKey)
			.collect(Collectors.joining(", "));

		return "This issue is blocked by: %s. Resolve blocking issues first.".formatted(blockingKeys);
	}

	@Override
	public GuardType getType() {
		return GuardType.NOT_BLOCKED;
	}
}
