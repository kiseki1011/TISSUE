package com.tissue.member.adapter.web.request;

import jakarta.validation.constraints.NotBlank;

public record LinkOAuthAccountRequest(@NotBlank String registerToken) {

}
