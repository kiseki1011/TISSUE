package com.tissue.feature.member.web.request;

import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_REGEX;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Update member name request")
public record UpdateMemberNameRequest(
        @Schema(description = "New name", example = "John Doe")
        @NotBlank
        @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        @Pattern(regexp = NAME_REGEX)
        String newName) {}
