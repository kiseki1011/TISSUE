package com.tissue.feature.workflow.application.dto;

import com.tissue.shared.vo.Name;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record TransitionDefinition(
        NodeIdentifier identifier,
        @Nullable Name name,
        @Nullable String description,
        NodeIdentifier sourceIdentifier,
        NodeIdentifier targetIdentifier) {}
