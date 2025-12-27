package com.tissue.workspace.adapter.in.web.dto.request;

import com.tissue.common.validator.annotation.pattern.DisplayNamePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequest(
        @DisplayNamePattern @Size(min = 2, max = 24) @NotBlank String displayName) {}
