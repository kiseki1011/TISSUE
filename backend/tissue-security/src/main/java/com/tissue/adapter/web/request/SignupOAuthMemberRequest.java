package com.tissue.adapter.web.request;

import com.tissue.application.dto.command.SignupOAuthMemberCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupOAuthMemberRequest(
        @NotBlank String registerToken,
        @NotBlank @Size(min = 4, max = 32) String username,
        @NotBlank @Size(min = 2, max = 50) String name) {

    public SignupOAuthMemberCommand toCommand() {
        return new SignupOAuthMemberCommand(registerToken, username, name);
    }
}
