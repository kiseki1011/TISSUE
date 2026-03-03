package com.tissue.feature.workflow.application.dto.request;

import com.tissue.feature.workflow.application.dto.GuardConfigData;
import java.util.List;
import lombok.Builder;

@Builder
public record ConfigureTransitionGuardsCommand(List<GuardConfigData> guards) {}
