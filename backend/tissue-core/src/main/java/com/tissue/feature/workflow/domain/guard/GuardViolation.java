package com.tissue.feature.workflow.domain.guard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(
        name = "GuardViolation",
        description = "A failed transition guard. Which guard blocked, reason, and structured details.")
public record GuardViolation(GuardType guardType, String message, Map<String, Object> details) {}
