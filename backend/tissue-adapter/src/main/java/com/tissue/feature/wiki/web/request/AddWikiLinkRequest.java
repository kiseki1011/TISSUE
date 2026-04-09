package com.tissue.feature.wiki.web.request;

import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import jakarta.validation.constraints.NotNull;

public record AddWikiLinkRequest(
        @NotNull WikiLinkTargetType targetType, @NotNull Long targetId) {}
