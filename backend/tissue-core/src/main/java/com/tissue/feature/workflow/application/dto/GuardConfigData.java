package com.tissue.feature.workflow.application.dto;

import com.tissue.feature.workflow.domain.guard.GuardType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record GuardConfigData(
        @NotNull GuardType guardType,
        @Nullable Map<String, Object> params,
        @NotNull Integer order) {}
