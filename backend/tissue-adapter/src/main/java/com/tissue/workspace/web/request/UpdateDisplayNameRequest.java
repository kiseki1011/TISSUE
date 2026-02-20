package com.tissue.workspace.web.request;

import static com.tissue.feature.workspace.domain.policy.WorkspaceMemberConstraintPolicy.DISPLAY_NAME_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceMemberConstraintPolicy.DISPLAY_NAME_MIN_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceMemberConstraintPolicy.DISPLAY_NAME_REGEX;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequest(
        @NotBlank
        @Size(min = DISPLAY_NAME_MIN_LENGTH, max = DISPLAY_NAME_MAX_LENGTH)
        @Pattern(regexp = DISPLAY_NAME_REGEX)
        String displayName) {}
