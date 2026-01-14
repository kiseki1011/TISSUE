package com.tissue.issue.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(@NotNull Boolean approved) {}
