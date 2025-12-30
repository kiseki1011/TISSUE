package com.tissue.workflow.application.dto;

import com.tissue.common.vo.Name;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record TransitionDefinition(
        @Nullable EntityRef transitionRef,
        Name name,
        @Nullable String description,
        EntityRef sourceStateRef,
        EntityRef targetStateRef) {}
