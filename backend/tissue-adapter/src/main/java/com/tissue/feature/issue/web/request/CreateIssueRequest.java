package com.tissue.feature.issue.web.request;

import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.CONTENT_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.SUMMARY_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.issue.domain.policy.IssueConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.domain.enums.IssuePriority;
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
        @NotNull IssuePriority priority,
        @Nullable Instant dueAt,
        @Nullable Integer storyPoint,
        @NotNull Long issueTypeId,
        @Nullable @Size(max = 50) Map<Long, Object> customFields,
        @Nullable Long assigneeMemberId) {

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
                .build();
    }
}
