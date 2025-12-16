package com.tissue.api.workflow.domain.guard.types;

import static com.tissue.api.workflow.domain.enums.StateCategory.*;
import static com.tissue.api.workflow.domain.guard.GuardType.*;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workflow.domain.guard.GuardContext;
import com.tissue.api.workflow.domain.guard.GuardType;
import com.tissue.api.workflow.domain.guard.TransitionGuard;

@Component
public class NotBlockedGuard implements TransitionGuard {

	@Override
	public GuardType getType() {
		return NOT_BLOCKED;
	}

	@Override
	public void evaluate(GuardContext context) {
		Issue issue = context.getIssue();

		List<Issue> blockingIssues = issue.getRelations().getBlockedByIssues();

		if (blockingIssues.isEmpty()) {
			return;
		}

		List<String> unresolvedKeys = blockingIssues.stream()
			.filter(blocking -> !blocking.getCurrentState().isCategorizedAs(DONE))
			.map(Issue::getKey)
			.toList();

		if (!unresolvedKeys.isEmpty()) {
			// TODO: 예외 개선
			//  - WorkflowErrorCode.TRANSITION_GUARD_FAILED
			//  - "This issue is blocked by: %s. Resolve blocking issues first."
			throw new RuntimeException("Transition blocked by unresolved issues");
			// .addContext("guardType", getType())
			// .addContext("reason", "BLOCKED_BY_ISSUES")
			// .addContext("blockingKeys", unresolvedKeys);
		}
	}

	@Override
	public void validateParams(Map<String, Object> params) {
	}
}
