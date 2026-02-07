package com.tissue.issue.adapter.web.request;

import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(@NotNull Boolean approved) {}
