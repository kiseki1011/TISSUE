package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to elevate the permission for sensitive operations.")
public record PermissionRequest(@NotBlank String password) {}
