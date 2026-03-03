package com.tissue.feature.workflow.application.dto.request;

import com.tissue.feature.workflow.application.dto.StateDefinition;
import com.tissue.feature.workflow.application.dto.TransitionDefinition;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateWorkflowCommand(
        Name name,
        @Nullable String description,
        ColorType color,
        List<StateDefinition> stateDefinitions,
        List<TransitionDefinition> transitionDefinitions) {}
