package com.tissue.feature.workflow.application.dto;

import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import org.jspecify.annotations.Nullable;

public record CreateStateDefinition(
        String tempKey, Name name, @Nullable String description, ColorType color, StateCategory category) {}
