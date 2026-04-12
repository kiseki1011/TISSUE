package com.tissue.feature.issue.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PerformTransitionRequest(
        @Schema(description = "ID of the workflow transition to execute") @NotNull
        Long transitionId) {}
