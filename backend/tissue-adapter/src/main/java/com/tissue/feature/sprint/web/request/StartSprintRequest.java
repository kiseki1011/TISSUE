package com.tissue.feature.sprint.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record StartSprintRequest(
        @Schema(description = "Due date of the sprint") @NotNull
        Instant dueAt) {}
