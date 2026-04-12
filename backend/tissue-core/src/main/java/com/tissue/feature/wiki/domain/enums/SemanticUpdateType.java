package com.tissue.feature.wiki.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "[Semantic version](https://semver.org/) bump for wiki document edits.")
public enum SemanticUpdateType {
    MAJOR,
    MINOR,
    PATCH
}
