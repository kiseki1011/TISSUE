package com.tissue.feature.wiki.adapter.web.request;

import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AddWikiLinkRequest(
        @NotNull WikiLinkTargetType targetType,

        @Schema(description = "ID of the target resource (issue ID, project ID, or wiki document ID)") @NotNull
        Long targetId) {}
