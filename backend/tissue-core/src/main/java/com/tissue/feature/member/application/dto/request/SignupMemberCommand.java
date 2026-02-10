package com.tissue.feature.member.application.dto.request;

import com.tissue.feature.member.domain.AuthProvider;
import lombok.Builder;

@Builder
public record SignupMemberCommand(
        AuthProvider provider, String email, String username, String password, String name, String signupToken) {}
