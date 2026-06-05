package com.tissue.security.adapter.web.request;

import com.tissue.security.domain.PatScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreatePatRequest(
        @Schema(description = "Label for this token", example = "ci-runner") @NotBlank @Size(max = 50)
        String name,

        @Schema(description = "Permission scope of the token", example = "READ_WRITE") @NotNull
        PatScope scope,

        @Schema(description = "Days until expiry (omit for no expiry)", example = "90") @Nullable @Positive @Max(365)
        Integer ttlDays) {}
