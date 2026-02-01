package com.tissue.issuetype.adapter.web.request;

import com.tissue.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record PatchIssueFieldRequest(
    JsonNullable<@Size(max = 255) String> description, JsonNullable<Boolean> required) {

    public PatchIssueFieldCommand toCommand(Long issueTypeId, Long issueFieldId,
        ProjectMemberContext actorContext) {
        return PatchIssueFieldCommand.builder()
                                     .issueTypeId(issueTypeId)
                                     .issueFieldId(issueFieldId)
                                     .description(description)
                                     .required(required)
                                     .actorContext(actorContext)
                                     .build();
    }
}
