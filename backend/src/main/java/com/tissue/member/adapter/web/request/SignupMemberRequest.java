package com.tissue.member.adapter.web.request;

import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.domain.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupMemberRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 20) String username,
        @NotBlank @Size(min = 8, max = 30) String password,
        @NotBlank @Size(min = 2, max = 50) String name,
        @NotBlank String signupToken) {

    public SignupMemberCommand toCommand() {
        return SignupMemberCommand.builder()
                .provider(AuthProvider.EMAIL)
                .email(email)
                .username(username)
                .password(password)
                .name(name)
                .signupToken(signupToken)
                .build();
    }
}
