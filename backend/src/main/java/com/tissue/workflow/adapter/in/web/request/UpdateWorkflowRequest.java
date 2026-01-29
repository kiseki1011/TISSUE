package com.tissue.workflow.adapter.in.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.common.util.JsonNullables;
import com.tissue.common.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.request.UpdateWorkflowCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateWorkflowRequest(
        JsonNullable<@NotBlank @Size(max = 32) String> name,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<@NotNull ColorType> color) {

    public UpdateWorkflowCommand toCommand(Long workflowId, ProjectMemberContext actorContext) {
        return UpdateWorkflowCommand.builder()
                .workflowId(workflowId)
                .name(JsonNullables.map(name, Name::of))
                .description(description)
                .color(color)
                .actorContext(actorContext)
                .build();
    }
}
