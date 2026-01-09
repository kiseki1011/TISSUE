package com.tissue.member.adapter.in.web.dto.request;

import com.tissue.common.validator.annotation.pattern.NamePattern;
import com.tissue.common.validator.annotation.pattern.UsernamePattern;
import com.tissue.member.application.dto.request.SignupOAuthMemberCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupOAuthMemberRequest(
        @NotBlank String registerToken,
        @NotBlank @UsernamePattern @Size(min = 4, max = 32) String username,
        @NotBlank @NamePattern @Size(min = 2, max = 50) String name) {
    public SignupOAuthMemberCommand toCommand() {
        return new SignupOAuthMemberCommand(registerToken, username, name);
    }
}
