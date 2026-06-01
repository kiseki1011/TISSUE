package com.tissue.feature.workflow.adapter.web.request;

import com.tissue.feature.workflow.application.dto.GuardConfigData;
import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(
        description = "Request to configure guards (preconditions) for a workflow transition. "
                + "Guards are evaluated in the specified order before allowing the transition.",
        example = """
        {
          "guards": [
            {
              "guardType": "ASSIGNEE_REQUIRED",
              "params": null,
              "order": 1
            },
            {
              "guardType": "NOT_BLOCKED",
              "params": null,
              "order": 2
            },
            {
              "guardType": "REQUIRED_APPROVAL",
              "params": { "requiredCount": 2 },
              "order": 3
            }
          ]
        }""")
public record ConfigureTransitionGuardsRequest(
        @NotNull @NotEmpty @Size(max = 20) List<@Valid GuardConfigData> guards) {

    public ConfigureTransitionGuardsCommand toCommand() {
        return ConfigureTransitionGuardsCommand.builder().guards(guards).build();
    }
}
