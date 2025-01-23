package com.tissue.api.position.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.NameSize;
import com.tissue.api.common.validator.annotation.size.text.StandardText;

import jakarta.validation.constraints.NotBlank;

public record UpdatePositionRequest(

	@NameSize
	@NotBlank(message = "{valid.notblank}")
	String name,

	@StandardText
	String description
) {
}
