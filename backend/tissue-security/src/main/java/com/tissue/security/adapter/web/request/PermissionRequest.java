package com.tissue.security.adapter.web.request;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequest(@NotBlank String password) {}
