package com.tissue.issue.adapter.in.web.dto.request;

import com.tissue.issue.application.dto.request.CreateIssueCommand;
import com.tissue.issue.domain.enums.IssuePriority;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record CreateIssueRequest(
        @NotBlank @Size(max = 100) String title,
        @Nullable @Size(max = 65535) String content,
        @Nullable @Size(max = 2000) String summary,
        @NotNull IssuePriority priority,
        @Nullable Instant dueAt,
        @Nullable Integer storyPoint,
        @NotNull Long issueTypeId,
        @Nullable Map<Long, Object> customFields,
        @Nullable Long assigneeMemberId) {

    public CreateIssueCommand toCommand(ProjectMemberContext actorContext) {
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
                .actorContext(actorContext)
                .build();
    }
}
