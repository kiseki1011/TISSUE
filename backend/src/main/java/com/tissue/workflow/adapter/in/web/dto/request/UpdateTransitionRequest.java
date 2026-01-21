package com.tissue.workflow.adapter.in.web.dto.request;

import com.tissue.common.util.JsonNullables;
import com.tissue.common.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.request.UpdateTransitionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTransitionRequest(
        JsonNullable<@NotBlank @Size(max = 32) String> name, JsonNullable<@Size(max = 255) String> description) {

    public UpdateTransitionCommand toCommand(Long workflowId, Long transitionId, ProjectMemberContext actorContext) {
        return UpdateTransitionCommand.builder()
                .workflowId(workflowId)
                .transitionId(transitionId)
                .name(JsonNullables.map(name, Name::of))
                .description(description)
                .actorContext(actorContext)
                .build();
    }
}
