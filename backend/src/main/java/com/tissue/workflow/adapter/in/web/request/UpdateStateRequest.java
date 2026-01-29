package com.tissue.workflow.adapter.in.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.common.util.JsonNullables;
import com.tissue.common.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.request.UpdateStateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateStateRequest(
        JsonNullable<@NotBlank @Size(max = 32) String> name,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<@NotNull ColorType> color) {

    public UpdateStateCommand toCommand(Long workflowId, Long stateId, ProjectMemberContext actorContext) {
        return UpdateStateCommand.builder()
                .workflowId(workflowId)
                .stateId(stateId)
                .name(JsonNullables.map(name, Name::of))
                .description(description)
                .color(color)
                .actorContext(actorContext)
                .build();
    }
}
