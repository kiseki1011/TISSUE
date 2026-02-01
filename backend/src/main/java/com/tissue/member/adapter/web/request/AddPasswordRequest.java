package com.tissue.member.adapter.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddPasswordRequest(
        @NotBlank @Size(min = 8, max = 100) String password) {}
