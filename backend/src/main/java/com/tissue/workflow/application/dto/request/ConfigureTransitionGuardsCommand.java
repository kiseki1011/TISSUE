package com.tissue.workflow.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.GuardConfigData;
import java.util.List;
import lombok.Builder;

@Builder
public record ConfigureTransitionGuardsCommand(
        Long workflowId, Long transitionId, List<GuardConfigData> guards, ProjectMemberContext actorContext) {}
