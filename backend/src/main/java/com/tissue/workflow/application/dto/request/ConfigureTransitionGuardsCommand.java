package com.tissue.workflow.application.dto.request;

import com.tissue.workflow.application.dto.GuardConfigData;
import java.util.List;
import lombok.Builder;

@Builder
public record ConfigureTransitionGuardsCommand(
        String workspaceKey,
        String projectKey,
        Long workflowId,
        Long transitionId,
        List<GuardConfigData> guards) {}
