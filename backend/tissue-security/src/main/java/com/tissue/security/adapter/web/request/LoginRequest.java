package com.tissue.security.adapter.web.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String identifier, @NotBlank String password) {}
