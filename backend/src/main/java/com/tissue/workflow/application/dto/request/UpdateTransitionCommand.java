package com.tissue.workflow.application.dto.request;

import com.tissue.common.vo.Name;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateTransitionCommand(
        String workspaceKey,
        String projectKey,
        Long workflowId,
        Long transitionId,
        JsonNullable<Name> name,
        JsonNullable<String> description) {}
