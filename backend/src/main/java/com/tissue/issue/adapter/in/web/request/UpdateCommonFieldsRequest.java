package com.tissue.issue.adapter.in.web.request;

import com.tissue.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.issue.domain.enums.IssuePriority;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateCommonFieldsRequest(
        JsonNullable<@NotBlank @Size(max = 100) String> title,
        JsonNullable<String> content,
        JsonNullable<String> summary,
        JsonNullable<IssuePriority> priority,
        JsonNullable<Instant> dueAt) {

    public UpdateCommonFieldsCommand toCommand(String issueKey, ProjectMemberContext actorContext) {
        return UpdateCommonFieldsCommand.builder()
                .issueKey(issueKey)
                .title(title)
                .content(content)
                .summary(summary)
                .priority(priority)
                .dueAt(dueAt)
                .actorContext(actorContext)
                .build();
    }
}
