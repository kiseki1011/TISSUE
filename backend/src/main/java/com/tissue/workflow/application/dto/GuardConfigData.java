package com.tissue.workflow.application.dto;

import com.tissue.workflow.domain.guard.GuardType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record GuardConfigData(
        @NotNull GuardType guardType,
        @Nullable Map<String, Object> params,
        @NotNull Integer order) {}
