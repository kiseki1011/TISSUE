package com.tissue.issue.adapter.web.request;

import com.tissue.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.issue.domain.enums.IssuePriority;
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

    public UpdateCommonFieldsCommand toCommand() {
        return UpdateCommonFieldsCommand.builder()
                .title(title)
                .content(content)
                .summary(summary)
                .priority(priority)
                .dueAt(dueAt)
                .build();
    }
}
