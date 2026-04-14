package com.tissue.feature.wiki.application.dto.request;

import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import org.jspecify.annotations.Nullable;

public record UpdateDocumentContentCommand(
        String content,
        SemanticUpdateType versionUpdateType,
        @Nullable String editReason) {}
