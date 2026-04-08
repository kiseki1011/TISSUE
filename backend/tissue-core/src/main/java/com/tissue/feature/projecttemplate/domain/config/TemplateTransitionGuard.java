package com.tissue.feature.projecttemplate.domain.config;

import com.tissue.feature.workflow.domain.guard.GuardType;
import java.util.Map;

public record TemplateTransitionGuard(GuardType guardType, Map<String, Object> guardParams, int executionOrder) {}
