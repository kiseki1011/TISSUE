package com.tissue.member.adapter.in.web.dto.request;

import com.tissue.common.validator.annotation.pattern.UsernamePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberUsernameRequest(
        @NotBlank @UsernamePattern @Size(min = 4, max = 32) String newUsername) {}
