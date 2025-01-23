package com.tissue.api.security.authentication.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.ShortText;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginRequest(

	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String identifier,

	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String password
) {
}
