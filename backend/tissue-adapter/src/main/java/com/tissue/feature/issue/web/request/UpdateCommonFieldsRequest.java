package com.tissue.feature.issue.web.request;

import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.CONTENT_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.SUMMARY_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

@Schema(
        description = "Partial update for common issue fields. Only provided fields are updated. "
                + "Set a field to null to clear its value.",
        example = """
        {
          "title": "Updated title",
          "priority": "BLOCKER",
          "dueAt": null
        }""")
public record UpdateCommonFieldsRequest(
        @Schema(description = "Cannot be empty when provided")
        JsonNullable<@NotBlank @Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH) String> title,

        JsonNullable<@Size(max = CONTENT_MAX_LENGTH) String> content,
        JsonNullable<@Size(max = SUMMARY_MAX_LENGTH) String> summary,
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
