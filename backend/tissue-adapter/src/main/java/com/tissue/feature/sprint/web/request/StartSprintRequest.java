package com.tissue.feature.sprint.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record StartSprintRequest(@NotNull Instant dueAt) {}
