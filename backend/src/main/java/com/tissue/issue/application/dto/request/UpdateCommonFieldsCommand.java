package com.tissue.issue.application.dto.request;

import com.tissue.issue.domain.enums.IssuePriority;
import java.time.Instant;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateCommonFieldsCommand(
        String workspaceKey,
        String projectKey,
        String issueKey,
        JsonNullable<String> title,
        JsonNullable<String> content,
        JsonNullable<String> summary,
        JsonNullable<IssuePriority> priority,
        JsonNullable<Instant> dueAt,
        Long actorMemberId) {}
