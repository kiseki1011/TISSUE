package com.tissue.issue.web.request;

import jakarta.validation.constraints.NotNull;

public record PerformTransitionRequest(@NotNull Long transitionId) {}
