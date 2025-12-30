package com.tissue.workflow.application.dto;

import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.workflow.domain.enums.StateCategory;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record StateDefinition(
        NodeIdentifier identifier, Name name, @Nullable String description, ColorType color, StateCategory category) {}
