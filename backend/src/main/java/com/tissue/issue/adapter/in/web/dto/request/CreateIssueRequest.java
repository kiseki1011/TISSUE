package com.tissue.issue.adapter.in.web.dto.request;

import com.tissue.common.validator.annotation.size.ContentText;
import com.tissue.common.validator.annotation.size.LongText;
import com.tissue.issue.application.dto.request.CreateIssueCommand;
import com.tissue.issue.domain.enums.IssuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import org.springframework.lang.Nullable;

public record CreateIssueRequest(
        @NotBlank @Size(max = 100) String title,
        @Nullable @ContentText String content,
        @Nullable @LongText String summary,
        @Nullable IssuePriority priority,
        @Nullable Instant dueAt,
        @NotNull Long issueTypeId,
        @Nullable Map<Long, Object> customFields,
        @Nullable Long assigneeMemberId) {
    public CreateIssueCommand toCommand(
            String workspaceKey, String projectKey, Long currentMemberId) {
        return CreateIssueCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .title(title)
                .content(content)
                .summary(summary)
                .priority(priority)
                .dueAt(dueAt)
                .issueTypeId(issueTypeId)
                .customFields(customFields == null ? Map.of() : customFields)
                .actorMemberId(currentMemberId)
                .assigneeMemberId(assigneeMemberId)
                .build();
    }
}
