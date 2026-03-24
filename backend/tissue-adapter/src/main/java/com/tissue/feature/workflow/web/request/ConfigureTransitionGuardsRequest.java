package com.tissue.feature.workflow.web.request;

import com.tissue.feature.workflow.application.dto.GuardConfigData;
import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ConfigureTransitionGuardsRequest(
        @NotNull @NotEmpty @Size(max = 20) List<@Valid GuardConfigData> guards) {

    public ConfigureTransitionGuardsCommand toCommand() {
        return ConfigureTransitionGuardsCommand.builder().guards(guards).build();
    }
}
