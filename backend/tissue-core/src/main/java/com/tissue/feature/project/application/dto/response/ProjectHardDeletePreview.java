package com.tissue.feature.project.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Counts of resources that a project hard-delete removes")
@Builder
public record ProjectHardDeletePreview(
        @Schema(description = "Target project key", example = "PROJ")
        String projectKey,

        @Schema(example = "42") long issues,
        @Schema(example = "120") long comments,

        @Schema(description = "Issue attachment rows (each backed by one stored file)", example = "15")
        long attachments,

        @Schema(example = "15") long files,
        @Schema(example = "8") long sprints,
        @Schema(example = "10") long tags,
        @Schema(example = "6") long members,
        @Schema(example = "300") long activityLogs,
        @Schema(example = "1") long vcsIntegrations) {}
