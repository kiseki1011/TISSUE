package com.tissue.adapter.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberPasswordRequest(
        String originalPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword) {}
