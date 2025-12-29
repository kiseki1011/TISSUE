package com.tissue.workflow.application.dto;

import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.workflow.domain.enums.StateCategory;
import lombok.Builder;

@Builder
public record StateDefinition(
        EntityRef stateRef, Name name, String description, ColorType color, StateCategory category) {}
