package com.tissue.security.adapter.web.request;

import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_REGEX;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.USERNAME_REGEX;
import static com.tissue.security.domain.policy.AuthenticationConstraintPolicy.PASSWORD_MAX_LENGTH;
import static com.tissue.security.domain.policy.AuthenticationConstraintPolicy.PASSWORD_MIN_LENGTH;
import static com.tissue.security.domain.policy.AuthenticationConstraintPolicy.PASSWORD_PATTERN_MESSAGE;
import static com.tissue.security.domain.policy.AuthenticationConstraintPolicy.PASSWORD_REGEX;

import com.tissue.security.application.dto.command.SignupMemberCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

@Schema(description = "Email signup request")
public record SignupMemberRequest(
        @Schema(description = "Email address (required when `email-required` is enabled)", example = "user@example.com")
        @Nullable
        @Email
        String email,

        @Schema(description = "Unique username", example = "johndoe")
        @NotBlank
        @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH)
        @Pattern(regexp = USERNAME_REGEX)
        String username,

        @Schema(description = "Account password", example = "password1234!")
        @NotBlank
        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
        @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_PATTERN_MESSAGE)
        String password,

        @Schema(description = "Display name", example = "John Doe")
        @NotBlank
        @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        @Pattern(regexp = NAME_REGEX)
        String name,

        @Schema(description = "Email verification token (required when `email-required` is enabled)") @Nullable
        String verifiedToken) {

    public SignupMemberCommand toCommand() {
        return SignupMemberCommand.builder()
                .email(email)
                .username(username)
                .password(password)
                .name(name)
                .verifiedToken(verifiedToken)
                .build();
    }
}
