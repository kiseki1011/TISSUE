package com.tissue.member.application.dto.request;

import com.tissue.member.domain.AuthProvider;
import lombok.Builder;

@Builder
public record SignupMemberCommand(AuthProvider provider, String email, String username, String password, String name) {}
