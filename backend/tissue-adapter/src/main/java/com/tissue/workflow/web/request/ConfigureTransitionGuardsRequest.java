package com.tissue.workflow.web.request;

import com.tissue.feature.workflow.application.dto.GuardConfigData;
import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import java.util.List;

public record ConfigureTransitionGuardsRequest(List<GuardConfigData> guards) {

    public ConfigureTransitionGuardsCommand toCommand() {
        return ConfigureTransitionGuardsCommand.builder().guards(guards).build();
    }
}
