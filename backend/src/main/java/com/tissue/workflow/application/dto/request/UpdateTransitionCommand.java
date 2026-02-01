package com.tissue.workflow.application.dto.request;

import com.tissue.global.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateTransitionCommand(
        Long workflowId,
        Long transitionId,
        JsonNullable<Name> name,
        JsonNullable<String> description,
        ProjectMemberContext actorContext) {}
