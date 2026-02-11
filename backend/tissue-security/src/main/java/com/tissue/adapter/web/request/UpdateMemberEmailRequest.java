package com.tissue.adapter.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberEmailRequest(
        @NotBlank @Email @Size(min = 4, max = 255) String newEmail,
        @NotBlank String verificationToken) {}
