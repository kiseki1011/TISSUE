package com.tissue.feature.projecttemplate.domain.config;

import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.enums.ColorType;
import java.util.List;

public record TemplateWorkflow(
        String tempId,
        String name,
        String description,
        ColorType color,
        List<TemplateState> states,
        List<TemplateTransition> transitions,
        String initialStateName) {

    public static TemplateWorkflow from(Workflow w) {
        List<TemplateState> states = w.getStates().stream()
                .map(s -> new TemplateState(s.getName().toString(), s.getDescription(), s.getColor(), s.getCategory()))
                .toList();

        List<TemplateTransition> transitions =
                w.getTransitions().stream().map(TemplateTransition::from).toList();

        return new TemplateWorkflow(
                w.getId().toString(),
                w.getName(),
                w.getDescription(),
                w.getColor(),
                states,
                transitions,
                w.getInitialState().getName().toString());
    }
}
