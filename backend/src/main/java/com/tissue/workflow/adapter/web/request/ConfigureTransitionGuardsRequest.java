package com.tissue.workflow.adapter.web.request;

import com.tissue.workflow.application.dto.GuardConfigData;
import com.tissue.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import java.util.List;

public record ConfigureTransitionGuardsRequest(List<GuardConfigData> guards) {

    public ConfigureTransitionGuardsCommand toCommand() {
        return ConfigureTransitionGuardsCommand.builder().guards(guards).build();
    }
}
