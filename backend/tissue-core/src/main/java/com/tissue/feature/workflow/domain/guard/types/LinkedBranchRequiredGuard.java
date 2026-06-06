package com.tissue.feature.workflow.domain.guard.types;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workflow.domain.exception.LinkedBranchRequiredException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Blocks a transition until the issue has at least one linked VCS branch
 */
@Component
public class LinkedBranchRequiredGuard implements TransitionGuard {

    @Override
    public GuardType getType() {
        return GuardType.LINKED_BRANCH_REQUIRED;
    }

    @Override
    public void evaluate(GuardContext context) {
        Issue issue = context.getIssue();
        if (issue.getBranches().isEmpty()) {
            throw new LinkedBranchRequiredException(issue.getKey());
        }
    }

    @Override
    public void validateParams(Map<String, Object> params, GuardType guardType) {}
}
