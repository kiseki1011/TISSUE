package com.tissue.security.authentication.presentation.dto.request;

import com.tissue.common.validator.annotation.size.text.ShortText;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginRequest(

	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String loginEmail,

	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String password
) {
}
