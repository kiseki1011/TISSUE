package com.tissue.adapter.web.request;

import jakarta.validation.constraints.NotBlank;

public record LinkOAuthAccountRequest(@NotBlank String registerToken) {}
