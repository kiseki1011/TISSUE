package com.tissue.security.application.dto.command;

import com.tissue.security.domain.AuthenticationIdentityProvider;
import lombok.Builder;

@Builder
public record SignupMemberCommand(
        AuthenticationIdentityProvider provider,
        String email,
        String username,
        String password,
        String name,
        String verifiedToken) {}
