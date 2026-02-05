package com.tissue.workflow.application.dto.request;

import com.tissue.workflow.application.dto.StateDefinition;
import com.tissue.workflow.application.dto.TransitionDefinition;
import java.util.List;

public record ReplaceWorkflowGraphCommand(
        Long version, List<StateDefinition> stateDefinitions, List<TransitionDefinition> transitionDefinitions) {}
