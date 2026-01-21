package com.tissue.issuetype.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchIssueTypeCommand(
        String workspaceKey,
        String projectKey,
        Long issueTypeId,
        JsonNullable<String> description,
        JsonNullable<ColorType> color,
        ProjectMemberContext actorContext) {}
