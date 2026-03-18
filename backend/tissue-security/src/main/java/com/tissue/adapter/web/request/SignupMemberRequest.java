package com.tissue.adapter.web.request;

import static com.tissue.domain.policy.AuthenticationConstraintPolicy.PASSWORD_MAX_LENGTH;
import static com.tissue.domain.policy.AuthenticationConstraintPolicy.PASSWORD_MIN_LENGTH;
import static com.tissue.domain.policy.AuthenticationConstraintPolicy.PASSWORD_PATTERN_MESSAGE;
import static com.tissue.domain.policy.AuthenticationConstraintPolicy.PASSWORD_REGEX;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_REGEX;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_REGEX;

import com.tissue.application.dto.command.SignupMemberCommand;
import com.tissue.domain.AuthenticationProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupMemberRequest(
        @NotBlank @Email String email,

        @NotBlank @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH) @Pattern(regexp = USERNAME_REGEX)
        String username,

        @NotBlank
        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
        @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_PATTERN_MESSAGE)
        String password,

        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH) @Pattern(regexp = NAME_REGEX)
        String name,

        @NotBlank String signupToken) {

    public SignupMemberCommand toCommand() {
        return SignupMemberCommand.builder()
                .provider(AuthenticationProvider.EMAIL)
                .email(email)
                .username(username)
                .password(password)
                .name(name)
                .signupToken(signupToken)
                .build();
    }
}
