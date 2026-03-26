package com.tissue.security.application.dto.command;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record SignupMemberCommand(
        @Nullable String email,
        String username,
        String password,
        String name,
        @Nullable String verifiedToken) {}
