package com.tissue.security.application.dto;

import com.tissue.feature.member.domain.Member;
import org.jspecify.annotations.Nullable;

/**
 * The identity claims extracted from a validated IdP ID token, used to resolve or provision a {@link Member}.
 *
 * @param subject   the IdP's stable {@code sub} claim (the durable identity key)
 * @param email     the email, must be unique within Tissue
 * @param username  the username, must be unique within Tissue
 * @param name      the display name
 */
public record OidcUserInfo(
        String subject,
        @Nullable String email,
        String username,
        @Nullable String name) {}
