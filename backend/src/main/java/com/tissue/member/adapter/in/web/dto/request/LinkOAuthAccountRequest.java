package com.tissue.member.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LinkOAuthAccountRequest(@NotBlank String registerToken) {}
