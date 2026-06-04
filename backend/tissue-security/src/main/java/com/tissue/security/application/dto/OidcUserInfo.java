package com.tissue.security.application.dto;

import org.jspecify.annotations.Nullable;

public record OidcUserInfo(
        String subject,
        @Nullable String email,
        @Nullable String username,
        @Nullable String name) {}
