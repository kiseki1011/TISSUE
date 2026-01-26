package com.tissue.member.adapter.in.web.dto.request;

import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.domain.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SignupMemberRequest(
        @NotBlank @Email @Size(min = 4, max = 255) String email,
        @NotBlank @Size(min = 4, max = 32) String username,
        @NotBlank @Size(min = 4, max = 100) String password,
        @NotBlank @Size(min = 2, max = 50) String name) {

    public SignupMemberCommand toCommand() {
        return SignupMemberCommand.builder()
                .provider(AuthProvider.EMAIL)
                .email(email.trim())
                .password(password)
                .username(username.trim())
                .name(name.trim())
                .build();
    }
}
