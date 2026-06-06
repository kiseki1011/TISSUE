package com.tissue.feature.workflow.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workflow.domain.TransitionGuardConfig;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.exception.AssigneeRequiredException;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workflow.domain.guard.GuardViolation;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransitionGuardEvaluatorTest {

    @Test
    @DisplayName("success: collectViolations maps a guard failure to a structured GuardViolation")
    void collectViolationsReturnsStructuredViolation() {
        Issue issue = mock(Issue.class);
        when(issue.getProjectKey()).thenReturn("PROJ");

        TransitionGuard guard = mock(TransitionGuard.class);
        doThrow(new AssigneeRequiredException("PROJ-1")).when(guard).evaluate(any());

        TransitionGuardEvaluator evaluator = evaluatorFor(GuardType.ASSIGNEE_REQUIRED, guard);

        List<GuardViolation> violations =
                evaluator.collectViolations(issue, transition(GuardType.ASSIGNEE_REQUIRED), 1L);

        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.guardType()).isEqualTo(GuardType.ASSIGNEE_REQUIRED);
            assertThat(v.message()).contains("assignee is required");
        });
    }

    private TransitionGuardEvaluator evaluatorFor(GuardType type, TransitionGuard guard) {
        TransitionGuardRegistry registry = mock(TransitionGuardRegistry.class);
        when(registry.getGuard(type)).thenReturn(guard);
        return new TransitionGuardEvaluator(registry);
    }

    private WorkflowTransition transition(GuardType type) {
        TransitionGuardConfig config = mock(TransitionGuardConfig.class);
        when(config.getGuardType()).thenReturn(type);
        when(config.getGuardParams()).thenReturn(Map.of());

        WorkflowTransition transition = mock(WorkflowTransition.class);
        when(transition.getGuardConfigs()).thenReturn(List.of(config));
        return transition;
    }
}
