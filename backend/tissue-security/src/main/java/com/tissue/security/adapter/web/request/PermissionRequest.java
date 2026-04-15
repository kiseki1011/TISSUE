package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to elevate the permission for sensitive operations.")
public record PermissionRequest(@NotBlank @Size(max = 100) String password) {}
