package com.tissue.feature.workflow.application.dto;

import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record StateDefinition(
        NodeIdentifier identifier, Name name, @Nullable String description, ColorType color, StateCategory category) {}
