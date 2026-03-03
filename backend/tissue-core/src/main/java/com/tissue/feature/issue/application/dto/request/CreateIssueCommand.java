package com.tissue.feature.issue.application.dto.request;

import com.tissue.feature.issue.domain.enums.IssuePriority;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateIssueCommand(
        Long sprintId,
        String parentProjectKey,
        @Nullable String parentKey,
        String title,
        @Nullable String content,
        @Nullable String summary,
        IssuePriority priority,
        @Nullable Instant dueAt,
        @Nullable Integer storyPoint,
        Long issueTypeId,
        Map<Long, Object> customFields,
        @Nullable Long assigneeMemberId) {}
