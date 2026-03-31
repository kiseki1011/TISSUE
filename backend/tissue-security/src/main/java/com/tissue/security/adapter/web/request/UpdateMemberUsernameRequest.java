package com.tissue.security.adapter.web.request;

import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_REGEX;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Update username request")
public record UpdateMemberUsernameRequest(
        @Schema(description = "New username", example = "newusername")
        @NotBlank
        @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH)
        @Pattern(regexp = USERNAME_REGEX)
        String newUsername) {}
