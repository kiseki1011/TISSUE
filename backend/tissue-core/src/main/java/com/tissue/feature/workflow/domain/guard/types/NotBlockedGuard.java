package com.tissue.feature.workflow.domain.guard.types;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workflow.domain.exception.TransitionGuardFailedException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotBlockedGuard implements TransitionGuard {

    @Override
    public GuardType getType() {
        return GuardType.NOT_BLOCKED;
    }

    @Override
    public void evaluate(GuardContext context) {
        Issue issue = context.getIssue();

        List<Issue> blockingIssues = issue.getRelations().getBlockedByIssues();

        if (blockingIssues.isEmpty()) {
            return;
        }

        List<String> unresolvedKeys = blockingIssues.stream()
                .filter(blocking -> !blocking.getCurrentState().getCategory().isTerminal())
                .map(Issue::getKey)
                .toList();

        if (!unresolvedKeys.isEmpty()) {
            throw new TransitionGuardFailedException(
                    getType(),
                    "This issue is blocked by: %s. Resolve blocking issues first.".formatted(unresolvedKeys),
                    issue.getKey(),
                    context.getWorkspaceKey());
        }
    }

    @Override
    public void validateParams(Map<String, Object> params, GuardType guardType) {}
}
