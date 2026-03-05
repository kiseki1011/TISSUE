package com.tissue.feature.workflow.application.dto;

import com.tissue.shared.vo.Name;
import org.jspecify.annotations.Nullable;

public record CreateTransitionDefinition(
        Name name, @Nullable String description, String sourceTempKey, String targetTempKey) {}
