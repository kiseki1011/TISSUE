package com.tissue.workflow.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.global.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.StateDefinition;
import com.tissue.workflow.application.dto.TransitionDefinition;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateWorkflowCommand(
    Name name,
    @Nullable String description,
    ColorType color,
    List<StateDefinition> stateDefinitions,
    List<TransitionDefinition> transitionDefinitions,
    ProjectMemberContext actorContext) {

}
