package com.tissue.adapter.web.request;

import static com.tissue.domain.policy.AuthenticationConstraintPolicy.PASSWORD_MAX_LENGTH;
import static com.tissue.domain.policy.AuthenticationConstraintPolicy.PASSWORD_MIN_LENGTH;
import static com.tissue.domain.policy.AuthenticationConstraintPolicy.PASSWORD_REGEX;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetToken,

        @NotBlank @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH) @Pattern(regexp = PASSWORD_REGEX)
        String newPassword) {}
