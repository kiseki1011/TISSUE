package com.tissue.feature.workflow.application.dto.request;

import com.tissue.feature.workflow.application.dto.CreateStateDefinition;
import com.tissue.feature.workflow.application.dto.CreateTransitionDefinition;
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
        List<CreateStateDefinition> stateDefinitions,
        List<CreateTransitionDefinition> transitionDefinitions) {}
