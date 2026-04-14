package com.tissue.security.adapter.web.request;

import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_REGEX;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberUsernameRequest(
        @NotBlank @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH) @Pattern(regexp = USERNAME_REGEX)
        String newUsername) {}
