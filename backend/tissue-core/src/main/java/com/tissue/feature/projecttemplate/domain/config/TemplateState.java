package com.tissue.feature.projecttemplate.domain.config;

import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;

public record TemplateState(String name, String description, ColorType color, StateCategory category) {}
