package com.tissue.issuetype.adapter.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateIssueTypeRequest(JsonNullable<@Size(max = 255) String> description,
                                     JsonNullable<ColorType> color) {

    public PatchIssueTypeCommand toCommand(
        String workspaceKey, String projectKey, Long issueTypeId,
        ProjectMemberContext actorContext) {
        return PatchIssueTypeCommand.builder()
                                    .workspaceKey(workspaceKey)
                                    .projectKey(projectKey)
                                    .issueTypeId(issueTypeId)
                                    .description(description)
                                    .color(color)
                                    .actorContext(actorContext)
                                    .build();
    }
}
