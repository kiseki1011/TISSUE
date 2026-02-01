package com.tissue.member.adapter.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberUsernameRequest(
    @NotBlank @Size(min = 4, max = 32) String newUsername) {

}
