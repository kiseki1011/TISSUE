package com.tissue.application.dto.command;

import com.tissue.domain.AuthenticationProvider;
import lombok.Builder;

@Builder
public record SignupMemberCommand(
        AuthenticationProvider provider,
        String email,
        String username,
        String password,
        String name,
        String signupToken) {}
