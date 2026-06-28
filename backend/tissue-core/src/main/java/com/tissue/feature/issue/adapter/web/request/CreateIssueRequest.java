package com.tissue.feature.issue.adapter.web.request;

import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.CONTENT_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.SUMMARY_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record CreateIssueRequest(
        @NotBlank @Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH)
        String title,

        @Nullable @Size(max = CONTENT_MAX_LENGTH) String content,

        @Nullable @Size(max = SUMMARY_MAX_LENGTH) String summary,

        @Schema(example = "P2") @NotNull IssuePriority priority,
        @Nullable Instant dueAt,
        @Nullable Integer storyPoint,
        @NotNull Long issueTypeId,

        @Schema(description = "Custom fields are passed as a map of field ID to value.") @Nullable @Size(max = 50)
        Map<Long, Object> customFields,

        @Nullable Long assigneeMemberId,

        @Schema(
                description = "Key of the parent issue. Required when the issue type's hierarchy is "
                        + "SUBTASK or MICROTASK (those cannot be created standalone); the parent must be exactly "
                        + "one hierarchy level above.")
        @Nullable
        String parentIssueKey) {

    public CreateIssueCommand toCommand() {
        return CreateIssueCommand.builder()
                .title(title)
                .content(content)
                .summary(summary)
                .priority(priority)
                .dueAt(dueAt)
                .storyPoint(storyPoint)
                .issueTypeId(issueTypeId)
                .customFields(customFields == null ? Map.of() : customFields)
                .assigneeMemberId(assigneeMemberId)
                .parentKey(parentIssueKey)
                .build();
    }
}
