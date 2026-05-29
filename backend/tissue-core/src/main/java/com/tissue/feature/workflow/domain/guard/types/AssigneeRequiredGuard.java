package com.tissue.feature.workflow.domain.guard.types;

import com.tissue.feature.workflow.domain.exception.TransitionGuardFailedException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AssigneeRequiredGuard implements TransitionGuard {

    @Override
    public GuardType getType() {
        return GuardType.ASSIGNEE_REQUIRED;
    }

    @Override
    public void evaluate(GuardContext context) {
        if (context.getIssue().getParticipants().getAssignee() == null) {
            throw new TransitionGuardFailedException(
                    getType(),
                    "An assignee is required before this transition.",
                    context.getIssue().getKey());
        }
    }

    @Override
    public void validateParams(Map<String, Object> params, GuardType guardType) {}
}
