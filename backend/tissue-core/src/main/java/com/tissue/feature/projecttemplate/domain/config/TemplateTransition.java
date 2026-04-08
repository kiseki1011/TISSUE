package com.tissue.feature.projecttemplate.domain.config;

import com.tissue.feature.workflow.domain.WorkflowTransition;
import java.util.List;

public record TemplateTransition(
        String name,
        String description,
        String sourceStateName,
        String targetStateName,
        List<TemplateTransitionGuard> guards) {

    public static TemplateTransition from(WorkflowTransition t) {
        List<TemplateTransitionGuard> guards = t.getGuardConfigs().stream()
                .map(g -> new TemplateTransitionGuard(g.getGuardType(), g.getGuardParams(), g.getExecutionOrder()))
                .toList();

        return new TemplateTransition(
                t.getName().toString(),
                t.getDescription(),
                t.getSourceState().getName().toString(),
                t.getTargetState().getName().toString(),
                guards);
    }
}
