package com.tissue.security.adapter.web.request;

import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_REGEX;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_REGEX;

import com.tissue.security.application.dto.command.SignupOAuthMemberCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupOAuthMemberRequest(
        @Schema(description = "Register token from OAuth callback") @NotBlank
        String registerToken,

        @NotBlank @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH) @Pattern(regexp = USERNAME_REGEX)
        String username,

        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH) @Pattern(regexp = NAME_REGEX)
        String name) {

    public SignupOAuthMemberCommand toCommand() {
        return new SignupOAuthMemberCommand(registerToken, username, name);
    }
}
