package com.tissue.feature.issue.application.dto.request;

import com.tissue.feature.issue.domain.enums.IssuePriority;
import java.time.Instant;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateCommonFieldsCommand(
        JsonNullable<String> title,
        JsonNullable<String> content,
        JsonNullable<String> summary,
        JsonNullable<IssuePriority> priority,
        JsonNullable<Instant> dueAt) {}
