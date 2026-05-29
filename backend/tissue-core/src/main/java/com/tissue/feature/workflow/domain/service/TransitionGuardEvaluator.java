package com.tissue.feature.workflow.domain.service;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workflow.domain.TransitionGuardConfig;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Evaluates the guards configured on a workflow transition for a given issue and actor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransitionGuardEvaluator {

    private final TransitionGuardRegistry guardRegistry;

    /**
     * First failing guard throws exception.
     */
    public void executeOrThrow(Issue issue, WorkflowTransition transition, @Nullable Long actorMemberId) {
        List<TransitionGuardConfig> configs = transition.getGuardConfigs();
        if (configs.isEmpty()) {
            return;
        }
        log.debug("Evaluating {} guards for transition: {}", configs.size(), transition.getDisplayName());
        for (TransitionGuardConfig config : configs) {
            TransitionGuard guard = guardRegistry.getGuard(config.getGuardType());
            guard.evaluate(buildContext(issue, transition, config, actorMemberId));
        }
    }

    /**
     * Returns messages for every failing guard so the client can preview which transitions
     * are currently blocked and why.
     */
    public List<String> collectViolations(Issue issue, WorkflowTransition transition, @Nullable Long actorMemberId) {
        List<TransitionGuardConfig> configs = transition.getGuardConfigs();
        if (configs.isEmpty()) {
            return List.of();
        }
        List<String> reasons = new ArrayList<>();
        for (TransitionGuardConfig config : configs) {
            try {
                TransitionGuard guard = guardRegistry.getGuard(config.getGuardType());
                guard.evaluate(buildContext(issue, transition, config, actorMemberId));
            } catch (RuntimeException e) {
                reasons.add(
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }
        return reasons;
    }

    private static GuardContext buildContext(
            Issue issue, WorkflowTransition transition, TransitionGuardConfig config, @Nullable Long actorMemberId) {
        return GuardContext.builder()
                .issue(issue)
                .transition(transition)
                .projectKey(issue.getProjectKey())
                .actorMemberId(actorMemberId)
                .params(config.getGuardParams())
                .build();
    }
}
