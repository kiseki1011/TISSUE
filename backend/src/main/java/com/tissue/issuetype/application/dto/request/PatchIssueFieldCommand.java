package com.tissue.issuetype.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchIssueFieldCommand(
        Long issueTypeId,
        Long issueFieldId,
        JsonNullable<String> description,
        JsonNullable<Boolean> required,
        ProjectMemberContext actorContext) {}
