package com.tissue.member.adapter.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberNameRequest(
        @NotBlank @Size(min = 2, max = 50) String newName) {}
