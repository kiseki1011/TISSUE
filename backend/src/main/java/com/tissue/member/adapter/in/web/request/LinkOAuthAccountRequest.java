package com.tissue.member.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record LinkOAuthAccountRequest(@NotBlank String registerToken) {}
