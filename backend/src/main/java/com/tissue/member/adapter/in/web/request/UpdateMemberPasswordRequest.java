package com.tissue.member.adapter.in.web.request;

import com.tissue.common.validator.annotation.pattern.PasswordPattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberPasswordRequest(
        String originalPassword,
        @NotBlank @PasswordPattern @Size(min = 8, max = 100) String newPassword) {}
