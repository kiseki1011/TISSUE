package com.tissue.workflow.application.dto;

import com.tissue.common.vo.Name;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record TransitionDefinition(
        NodeIdentifier identifier,
        Name name,
        @Nullable String description,
        NodeIdentifier sourceIdentifier,
        NodeIdentifier targetIdentifier) {}
