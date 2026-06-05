package com.tissue.security.application.dto;

import com.tissue.security.domain.PersonalAccessToken;

public record GeneratedToken(PersonalAccessToken token, String rawToken) {}
