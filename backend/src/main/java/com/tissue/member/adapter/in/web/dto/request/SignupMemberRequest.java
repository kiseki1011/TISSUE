package com.tissue.member.adapter.in.web.dto.request;

import com.tissue.common.validator.annotation.pattern.NamePattern;
import com.tissue.common.validator.annotation.pattern.PasswordPattern;
import com.tissue.common.validator.annotation.pattern.UsernamePattern;
import com.tissue.member.application.dto.request.SignupMemberCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SignupMemberRequest(
        @NotBlank @Email @Size(min = 4, max = 255) String email,
        @NotBlank @UsernamePattern @Size(min = 4, max = 32) String username,
        @NotBlank @PasswordPattern @Size(min = 8, max = 100) String password,
        @NotBlank @NamePattern @Size(min = 2, max = 50) String name) {
    public SignupMemberCommand toCommand() {
        return SignupMemberCommand.builder()
                .email(email.trim())
                .password(password)
                .username(username.trim())
                .name(name.trim())
                .build();
    }
}
