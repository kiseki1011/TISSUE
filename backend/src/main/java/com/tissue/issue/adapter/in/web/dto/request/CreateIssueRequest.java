package com.tissue.issue.adapter.in.web.dto.request;

import com.tissue.issue.application.dto.request.CreateIssueCommand;
import com.tissue.issue.domain.enums.IssuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;

// TODO: where is story point? should i add story point?
public record CreateIssueRequest(
        @NotBlank @Size(max = 100) String title,
        @Nullable @Size(max = 65535) String content,
        @Nullable @Size(max = 2000) String summary,
        @NotNull IssuePriority priority,
        @Nullable Instant dueAt,
        @NotNull Long issueTypeId,
        @Nullable Map<Long, Object> customFields,
        @Nullable Long assigneeMemberId) {

    // TODO: need to change to match CreateIssueCommand
    public CreateIssueCommand toCommand(String workspaceKey, String projectKey, Long currentMemberId) {
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
