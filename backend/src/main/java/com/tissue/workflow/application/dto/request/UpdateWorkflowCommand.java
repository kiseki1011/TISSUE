package com.tissue.workflow.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.global.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateWorkflowCommand(
        Long workflowId,
        JsonNullable<Name> name,
        JsonNullable<String> description,
        JsonNullable<ColorType> color,
        ProjectMemberContext actorContext) {}
