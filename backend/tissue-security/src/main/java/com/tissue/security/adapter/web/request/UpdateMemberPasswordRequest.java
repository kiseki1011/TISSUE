package com.tissue.security.adapter.web.request;

import static com.tissue.security.domain.policy.AuthenticationConstraintPolicy.PASSWORD_MAX_LENGTH;
import static com.tissue.security.domain.policy.AuthenticationConstraintPolicy.PASSWORD_MIN_LENGTH;
import static com.tissue.security.domain.policy.AuthenticationConstraintPolicy.PASSWORD_PATTERN_MESSAGE;
import static com.tissue.security.domain.policy.AuthenticationConstraintPolicy.PASSWORD_REGEX;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Update password request")
public record UpdateMemberPasswordRequest(
        @Schema(description = "Current password for confirmation") @NotBlank @Size(max = 100)
        String originalPassword,

        @Schema(example = "newPassword1234!")
        @NotBlank
        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
        @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_PATTERN_MESSAGE)
        String newPassword) {}
